package delaterrealassiette;

import java.awt.Graphics2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Accès JDBC MariaDB et synchronisation facultative des objets du jeu. */
public class GestionBDD {
    private static final java.util.concurrent.ConcurrentMap<String,Boolean> ETATS_LEVIERS = new java.util.concurrent.ConcurrentHashMap<>();
    // === MULTIJOUEUR === La gestion historique des tuiles et joueurs est conservée.
    private static final int ID_CARTE = 1;
    private Connection connexion;
    private Joueur joueur;
    private Map carte;
    private String pseudo;

    /** Configure les objets utilisés par miseAJourImport et miseAJourExport. */
    public void configurer(Joueur joueur, String pseudo, Map carte) {
        this.joueur = joueur;
        this.pseudo = pseudo;
        this.carte = carte;
    }

    /** Ouvre une connexion MariaDB. Paramètres système laquete.db.* ou variables LAQUETE_DB_* . */
    public void demarrer() {
        try {
            String hote = parametre("laquete.db.host", "LAQUETE_DB_HOST", "nemrod.ens2m.fr");
            String port = parametre("laquete.db.port", "LAQUETE_DB_PORT", "3306");
            String base = parametre("laquete.db.name", "LAQUETE_DB_NAME", "2026-2027_s1_vs1_tp2_la_quete_de_philippe");
            String utilisateur = parametre("laquete.db.user", "LAQUETE_DB_USER", "etudiant");
            String motDePasse = parametre("laquete.db.password", "LAQUETE_DB_PASSWORD", "YTDTvj9TR3CDYCmP");
            String url = "jdbc:mariadb://" + hote + ":" + port + "/" + base;
            connexion = DriverManager.getConnection(url, utilisateur, motDePasse);
        } catch (SQLException e) {
            System.err.println("Impossible d'ouvrir la connexion MariaDB.");
            e.printStackTrace();
        }
    }

    private String parametre(String propriete, String variable, String valeurDefaut) {
        String valeur = System.getProperty(propriete);
        if (valeur == null || valeur.isEmpty()) {
            valeur = System.getenv(variable);
        }
        return (valeur == null || valeur.isEmpty()) ? valeurDefaut : valeur;
    }

    /** Ferme la connexion JDBC si elle est ouverte. */
    public void arreter() {
        if (connexion != null) {
            try {
                connexion.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connexion = null;
            }
        }
    }

    /** Charge l'état du joueur et, si fourni, les tuiles enregistrées de la carte. */
    public void miseAJourImport() {
        if (connexion == null || joueur == null || pseudo == null) {
            return;
        }
        String sql = "SELECT points_de_vie, points_de_vie_max, score, position_x, "
                + "position_y, avatar_actuel FROM joueur WHERE pseudo = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, pseudo);
            try (ResultSet resultat = requete.executeQuery()) {
                if (resultat.next()) {
                    int pvMax = resultat.getInt("points_de_vie_max");
                    // Le maximum est immuable dans Joueur; on ajuste les PV disponibles sans dépasser sa limite.
                    int pvImportes = Math.min(resultat.getInt("points_de_vie"), joueur.getPointsDeVieMax());
                    joueur.modifierPointsDeVie(pvImportes - joueur.getPointsDeVie());
                    joueur.ajouterScore(resultat.getInt("score") - joueur.getScore());
                    joueur.definirPosition(resultat.getInt("position_x"), resultat.getInt("position_y"));
                    changerAvatar(resultat.getString("avatar_actuel"));
                    if (pvMax != joueur.getPointsDeVieMax()) {
                        System.err.println("Attention : points_de_vie_max en BDD différent de Joueur.java; "
                                + "la limite en mémoire reste celle de Joueur.");
                    }
                }
            }
            importerTuiles();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void changerAvatar(String nom) {
        if ("Etchebest".equals(nom)) {
            joueur.changerChef(joueur.getEtchebest());
        } else if ("Lignac".equals(nom)) {
            joueur.changerChef(joueur.getLignac());
        } else if ("Tarayre".equals(nom)) {
            joueur.changerChef(joueur.getTarayre());
        }
    }

    private void importerTuiles() throws SQLException {
        if (carte == null) {
            return;
        }
        String sql = "SELECT indice_ligne, indice_colonne, numero_tuile FROM tuile WHERE carte_id = ?";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setInt(1, ID_CARTE);
            try (ResultSet resultat = requete.executeQuery()) {
                while (resultat.next()) {
                    carte.setTuile(resultat.getInt("indice_ligne"), resultat.getInt("indice_colonne"),
                            resultat.getInt("numero_tuile"));
                }
            }
        }
        carte.effacerModifie();
    }

    /** Enregistre l'état du joueur et le décor seulement lorsqu'il a été modifié. */
    public void miseAJourExport() {
        if (connexion == null) {
            return;
        }
        boolean autoCommit = true;
        try {
            autoCommit = connexion.getAutoCommit();
            connexion.setAutoCommit(false);
            if (joueur != null && pseudo != null) {
                exporterJoueur();
            }
            if (carte != null && carte.estModifie()) {
                exporterCarte();
            }
            connexion.commit();
            if (carte != null) {
                carte.effacerModifie();
            }
        } catch (SQLException e) {
            try {
                connexion.rollback();
            } catch (SQLException erreurRollback) {
                e.addSuppressed(erreurRollback);
            }
            e.printStackTrace();
        } finally {
            try {
                connexion.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void exporterJoueur() throws SQLException {
        String sql = "INSERT INTO joueur (pseudo, points_de_vie, points_de_vie_max, score, "
                + "position_x, position_y, avatar_actuel) VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE points_de_vie=VALUES(points_de_vie), "
                + "points_de_vie_max=VALUES(points_de_vie_max), score=VALUES(score), "
                + "position_x=VALUES(position_x), position_y=VALUES(position_y), "
                + "avatar_actuel=VALUES(avatar_actuel)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setString(1, pseudo);
            requete.setInt(2, joueur.getPointsDeVie());
            requete.setInt(3, joueur.getPointsDeVieMax());
            requete.setInt(4, joueur.getScore());
            requete.setInt(5, joueur.getPositionX());
            requete.setInt(6, joueur.getPositionY());
            requete.setString(7, joueur.getNomAvatarActuel());
            requete.executeUpdate();
        }
    }

    private void exporterCarte() throws SQLException {
        String majCarte = "UPDATE carte SET largeur = ?, hauteur = ?, taille_tuile = ? WHERE carte_id = ?";
        try (PreparedStatement requete = connexion.prepareStatement(majCarte)) {
            requete.setInt(1, carte.getLargeur());
            requete.setInt(2, carte.getHauteur());
            requete.setInt(3, carte.getTailleTuile());
            requete.setInt(4, ID_CARTE);
            requete.executeUpdate();
        }
        String sql = "INSERT INTO tuile (carte_id, indice_ligne, indice_colonne, numero_tuile) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE numero_tuile=VALUES(numero_tuile)";
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            for (int ligne = 0; ligne < carte.getHauteur(); ligne++) {
                for (int colonne = 0; colonne < carte.getLargeur(); colonne++) {
                    requete.setInt(1, ID_CARTE);
                    requete.setInt(2, ligne);
                    requete.setInt(3, colonne);
                    requete.setInt(4, carte.getTuile(ligne, colonne));
                    requete.addBatch();
                }
            }
            requete.executeBatch();
        }
    }
    // === MULTIJOUEUR === Création d'une partie et inscription atomique du premier joueur.
    public static InfosPartie heberger(String pseudo, String code, String motDePasse) throws SQLException {
        String id = UUID.randomUUID().toString();
        try (Connection c = ConnexionBDD.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO partie_multi(code_partie,mot_de_passe) VALUES (?,?)")) {
                p.setString(1, code); p.setString(2, motDePasse); p.executeUpdate();
            }
            inscrire(c, id, code, pseudo);
            c.commit();
            return new InfosPartie(pseudo, code, motDePasse, id, true);
        } catch (SQLException e) { throw e; }
    }

    // === MULTIJOUEUR === Vérifie la partie et son mot de passe avant l'inscription.
    public static InfosPartie rejoindre(String pseudo, String code, String motDePasse) throws SQLException {
        String id = UUID.randomUUID().toString();
        try (Connection c = ConnexionBDD.getInstance().getConnection()) {
            try (PreparedStatement p = c.prepareStatement("SELECT code_partie FROM partie_multi WHERE code_partie=? AND mot_de_passe=?")) {
                p.setString(1, code); p.setString(2, motDePasse);
                try (ResultSet r = p.executeQuery()) { if (!r.next()) throw new SQLException("Partie introuvable ou mot de passe incorrect."); }
            }
            inscrire(c, id, code, pseudo);
        }
        return new InfosPartie(pseudo, code, motDePasse, id);
    }

    private static void inscrire(Connection c, String id, String code, String pseudo) throws SQLException {
        String sql = "INSERT INTO joueur_multi(session_id,code_partie,pseudo,position_x,position_y,avatar_actuel,dernier_ping) VALUES (?,?,?,32,64,'Etchebest',CURRENT_TIMESTAMP(3))";
        try (PreparedStatement p = c.prepareStatement(sql)) { p.setString(1,id); p.setString(2,code); p.setString(3,pseudo); p.executeUpdate(); }
    }

    // === MULTIJOUEUR === Liste des participants en ligne destinée aux fenêtres de sélection.
    public static List<String> listerJoueurs(String code) throws SQLException {
        List<String> resultat = new ArrayList<>();
        String sql = "SELECT pseudo FROM joueur_multi WHERE code_partie=? AND dernier_ping >= CURRENT_TIMESTAMP(3) - INTERVAL 15 SECOND ORDER BY pseudo";
        try (Connection c=ConnexionBDD.getInstance().getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,code); try(ResultSet r=p.executeQuery()){ while(r.next()) resultat.add(r.getString(1)); }
        }
        return Collections.unmodifiableList(resultat);
    }


    // === MULTIJOUEUR === État persistant d'un ingrédient et de son type.
    static final class EtatIngredient {
        final int id, x, y; final TypeIngredient type; final boolean ramasse;
        EtatIngredient(int id, int x, int y, TypeIngredient type, boolean ramasse) {
            this.id=id; this.x=x; this.y=y; this.type=type; this.ramasse=ramasse;
        }
    }
    // === MULTIJOUEUR === Snapshot de monstre lu depuis monstre_multi.
    static final class EtatMonstre {
        final int id,x,y,pv; final boolean vivant,gauche;
        EtatMonstre(int id,int x,int y,int pv,boolean vivant,boolean gauche){this.id=id;this.x=x;this.y=y;this.pv=pv;this.vivant=vivant;this.gauche=gauche;}
    }
    private static void initialiserMonde(Connection c, InfosPartie i, Jeu jeu) throws SQLException {
        String code=i.getIdentifiantPartie(); int niv=jeu.getNumeroNiveau();
        String ins="INSERT IGNORE INTO ingredient_multi(code_partie,niveau,ingredient_id,x,y,type) VALUES (?,?,?,?,?,?)";
        try(PreparedStatement p=c.prepareStatement(ins)){int idx=0;for(Ingredient g:jeu.getIngredients()){
            p.setString(1,code);p.setInt(2,niv);p.setInt(3,idx++);p.setInt(4,g.getPositionX());p.setInt(5,g.getPositionY());p.setString(6,g.getTypeIngredient().name());p.addBatch();}p.executeBatch();}
        ins="INSERT IGNORE INTO monstre_multi(code_partie,niveau,monstre_id,type,x,y,pv,vivant,direction_gauche) VALUES (?,?,?,?,?,?,100,1,?)";
        try(PreparedStatement p=c.prepareStatement(ins)){for(Entite e:jeu.getEntites())if(e instanceof Monstre){Monstre m=(Monstre)e;p.setString(1,code);p.setInt(2,niv);p.setInt(3,m.getIndexMulti());p.setString(4,"RAT");p.setInt(5,m.getPositionX());p.setInt(6,m.getPositionY());p.setBoolean(7,m.isRegardeVersLaGauche());p.addBatch();}p.executeBatch();}
        ins="INSERT IGNORE INTO levier_multi(code_partie,niveau,levier_id,id_mecanisme,actif) VALUES (?,?,?,?,?)";
        try(PreparedStatement p=c.prepareStatement(ins)){for(Levier l:jeu.getLeviers()){p.setString(1,code);p.setInt(2,niv);p.setInt(3,l.getIndexMulti());p.setInt(4,l.getIdMecanisme());p.setBoolean(5,l.isActive());p.addBatch();}p.executeBatch();}
    }
    /** Publie les événements en lot à chaque cycle de 100 ms, sans attendre le polling du monde. */
    private static void publierEvenements(Connection c, InfosPartie i, Jeu jeu) throws SQLException {
        String code=i.getIdentifiantPartie(); int niv=jeu.getNumeroNiveau();
        Jeu.DemandeCollecte collecteEnCours;
        while ((collecteEnCours=jeu.getCollectesEnAttente().poll())!=null) {
            final int niveauCollecte=collecteEnCours.niveau;
            final int idCollecte=collecteEnCours.id;
            boolean accepte=false, refuseeParAutre=false, etatConnu=false;
            try {
                String sql="UPDATE ingredient_multi SET ramasse=1,ramasse_par=? WHERE code_partie=? AND niveau=? AND ingredient_id=? AND ramasse=0";
                try (PreparedStatement p=c.prepareStatement(sql)) {
                    p.setString(1,i.getPseudo()); p.setString(2,code); p.setInt(3,niveauCollecte); p.setInt(4,idCollecte);
                    accepte=p.executeUpdate()==1;
                } catch (SQLException e) {
                    if (!estColonneManquante(e, "ramasse")) throw e;
                    // Ancien schéma : ramasse_par est la source d'état et le verrou atomique.
                    String fallback="UPDATE ingredient_multi SET ramasse_par=? WHERE code_partie=? AND niveau=? AND ingredient_id=? AND (ramasse_par IS NULL OR ramasse_par='')";
                    try (PreparedStatement p=c.prepareStatement(fallback)) {
                        p.setString(1,i.getPseudo()); p.setString(2,code); p.setInt(3,niveauCollecte); p.setInt(4,idCollecte);
                        accepte=p.executeUpdate()==1;
                    }
                }
                if (accepte) {
                    etatConnu=true;
                } else {
                    String select="SELECT ramasse,ramasse_par FROM ingredient_multi WHERE code_partie=? AND niveau=? AND ingredient_id=?";
                    try (PreparedStatement p=c.prepareStatement(select)) {
                        p.setString(1,code); p.setInt(2,niveauCollecte); p.setInt(3,idCollecte);
                        try (ResultSet r=p.executeQuery()) {
                            if (r.next()) {
                                boolean ramasse=r.getBoolean("ramasse");
                                String proprietaire=r.getString("ramasse_par");
                                etatConnu=true;
                                if (ramasse || (proprietaire!=null && !proprietaire.isEmpty())) {
                                    accepte=proprietaire==null || proprietaire.isEmpty() || i.getPseudo().equals(proprietaire);
                                    refuseeParAutre=proprietaire!=null && !proprietaire.isEmpty() && !i.getPseudo().equals(proprietaire);
                                } else {
                                    // Ligne encore libre : ne pas annuler l'état local, retenter au prochain cycle.
                                    etatConnu=false;
                                }
                            }
                        }
                    } catch (SQLException e) {
                        if (!estColonneManquante(e, "ramasse")) throw e;
                        String fallback="SELECT ramasse_par FROM ingredient_multi WHERE code_partie=? AND niveau=? AND ingredient_id=?";
                        try (PreparedStatement p=c.prepareStatement(fallback)) {
                            p.setString(1,code); p.setInt(2,niveauCollecte); p.setInt(3,idCollecte);
                            try (ResultSet r=p.executeQuery()) {
                                if (r.next()) {
                                    String proprietaire=r.getString(1);
                                    if (proprietaire!=null && !proprietaire.isEmpty()) {
                                        etatConnu=true;
                                        accepte=i.getPseudo().equals(proprietaire);
                                        refuseeParAutre=!accepte;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erreur SQL lors de la collecte (partie="+code+", niveau="+niveauCollecte+", ingrédient="+idCollecte+"): "+e.getMessage());
                jeu.getCollectesEnAttente().add(collecteEnCours);
                throw e;
            }
            if (!etatConnu) {
                jeu.getCollectesEnAttente().add(collecteEnCours);
                break;
            }
            final boolean succes=accepte, refus=refuseeParAutre;
            javax.swing.SwingUtilities.invokeLater(()->jeu.resultatCollecte(niveauCollecte,idCollecte,succes,refus));
        }
        String deg="UPDATE monstre_multi SET vivant=IF(pv-?>0,1,0),pv=GREATEST(0,pv-?) WHERE code_partie=? AND niveau=? AND monstre_id=?";
        try(PreparedStatement p=c.prepareStatement(deg)){String evenementDegats;while((evenementDegats=jeu.getDegatsEnAttente().poll())!=null){String[] v=evenementDegats.split(":");int valeur=Integer.parseInt(v[1]);p.setInt(1,valeur);p.setInt(2,valeur);p.setString(3,code);p.setInt(4,niv);p.setInt(5,Integer.parseInt(v[0]));p.addBatch();}p.executeBatch();}
        String lev="UPDATE levier_multi SET actif=? WHERE code_partie=? AND niveau=? AND levier_id=? AND actif<>?";
        try(PreparedStatement p=c.prepareStatement(lev)){String evenementLevier;while((evenementLevier=jeu.getLeviersEnAttente().poll())!=null){String[] v=evenementLevier.split(":");p.setBoolean(1,Boolean.parseBoolean(v[1]));p.setString(2,code);p.setInt(3,niv);p.setInt(4,Integer.parseInt(v[0]));p.setBoolean(5,Boolean.parseBoolean(v[1]));p.addBatch();}p.executeBatch();}
    }

    private static boolean estColonneManquante(SQLException e, String colonne) {
        String message=e.getMessage();
        String m=message==null?"":message.toLowerCase(java.util.Locale.ROOT);
        return "42S22".equals(e.getSQLState()) && (m.isEmpty() || m.contains(colonne.toLowerCase(java.util.Locale.ROOT)))
                || (m.contains("unknown column") && m.contains(colonne.toLowerCase(java.util.Locale.ROOT)));
    }

    private static void ajouterEtatIngredient(int id,int x,int y,String nomType,boolean ramasse,
            List<EtatIngredient> ingredients,java.util.Map<TypeIngredient,Integer> quantites) throws SQLException {
        final TypeIngredient type;
        try { type=TypeIngredient.valueOf(nomType); }
        catch (IllegalArgumentException e) { throw new SQLException("Type d'ingrédient inconnu dans ingredient_multi: "+nomType,e); }
        ingredients.add(new EtatIngredient(id,x,y,type,ramasse));
        if(ramasse) quantites.put(type,quantites.getOrDefault(type,0)+1);
    }

    private static void synchroniserMonde(Connection c,InfosPartie i,Jeu jeu) throws SQLException {
        String code=i.getIdentifiantPartie(); int niv=jeu.getNumeroNiveau();
        if(i.estHebergeur()){
            // Relire les PV persistants puis publier la simulation locale autoritaire.
            List<EtatMonstre> avant=lireMonstres(c,code,niv); java.util.Map<Integer,Integer> pv=new java.util.HashMap<>();for(EtatMonstre e:avant)pv.put(e.id,e.pv);
            for(Entite e:jeu.getEntites())if(e instanceof Monstre){Monstre m=(Monstre)e;int vie=pv.getOrDefault(m.getIndexMulti(),100);m.appliquerDegats(Math.max(0,m.getPointsDeVie()-vie));}
            String up="UPDATE monstre_multi SET x=?,y=?,pv=?,vivant=?,direction_gauche=? WHERE code_partie=? AND niveau=? AND monstre_id=? AND (x<>? OR y<>? OR pv<>? OR vivant<>? OR direction_gauche<>?)";
            try(PreparedStatement p=c.prepareStatement(up)){for(Entite e:jeu.getEntites())if(e instanceof Monstre){Monstre m=(Monstre)e;p.setInt(1,m.getPositionX());p.setInt(2,m.getPositionY());p.setInt(3,m.getPointsDeVie());p.setBoolean(4,m.estVivant());p.setBoolean(5,m.isRegardeVersLaGauche());p.setString(6,code);p.setInt(7,niv);p.setInt(8,m.getIndexMulti());p.setInt(9,m.getPositionX());p.setInt(10,m.getPositionY());p.setInt(11,m.getPointsDeVie());p.setBoolean(12,m.estVivant());p.setBoolean(13,m.isRegardeVersLaGauche());p.addBatch();}p.executeBatch();}
        }
        final List<EtatIngredient> ingredients=new ArrayList<>();
        final java.util.Map<TypeIngredient,Integer> quantites=new java.util.EnumMap<>(TypeIngredient.class);
        String sel="SELECT ingredient_id,x,y,type,ramasse FROM ingredient_multi WHERE code_partie=? AND niveau=? ORDER BY ingredient_id";
        try (PreparedStatement p=c.prepareStatement(sel)) {
            p.setString(1,code); p.setInt(2,niv);
            try (ResultSet r=p.executeQuery()) {
                while(r.next()) ajouterEtatIngredient(r.getInt("ingredient_id"),r.getInt("x"),r.getInt("y"),
                        r.getString("type"),r.getBoolean("ramasse"),ingredients,quantites);
            }
        } catch (SQLException e) {
            if (!estColonneManquante(e,"ramasse")) throw e;
            System.err.println("Schéma ingredient_multi sans colonne ramasse; lecture de repli via ramasse_par.");
            String fallback="SELECT ingredient_id,x,y,type,ramasse_par FROM ingredient_multi WHERE code_partie=? AND niveau=? ORDER BY ingredient_id";
            try (PreparedStatement p=c.prepareStatement(fallback)) {
                p.setString(1,code); p.setInt(2,niv);
                try (ResultSet r=p.executeQuery()) {
                    while(r.next()) {
                        String proprietaire=r.getString("ramasse_par");
                        ajouterEtatIngredient(r.getInt("ingredient_id"),r.getInt("x"),r.getInt("y"),
                                r.getString("type"),proprietaire!=null && !proprietaire.isEmpty(),ingredients,quantites);
                    }
                }
            }
        }
        final List<EtatMonstre> monstres=lireMonstres(c,code,niv);
        final java.util.Map<Integer,Boolean> leviers=new java.util.HashMap<>();
        sel="SELECT levier_id,actif FROM levier_multi WHERE code_partie=? AND niveau=?";
        try(PreparedStatement p=c.prepareStatement(sel)){p.setString(1,code);p.setInt(2,niv);try(ResultSet r=p.executeQuery()){while(r.next())leviers.put(r.getInt(1),r.getBoolean(2));}}
        javax.swing.SwingUtilities.invokeLater(()->{jeu.appliquerIngredientsPartages(ingredients,quantites);if(!i.estHebergeur())jeu.appliquerMonstres(monstres);jeu.appliquerLeviersDistants(leviers);});
    }
    private static List<EtatMonstre> lireMonstres(Connection c,String code,int niv)throws SQLException{
        List<EtatMonstre> l=new ArrayList<>();String sql="SELECT monstre_id,x,y,pv,vivant,direction_gauche FROM monstre_multi WHERE code_partie=? AND niveau=?";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,code);p.setInt(2,niv);try(ResultSet r=p.executeQuery()){while(r.next())l.add(new EtatMonstre(r.getInt(1),r.getInt(2),r.getInt(3),r.getInt(4),r.getBoolean(5),r.getBoolean(6)));}}return l;
    }

    // État privé au thread de synchronisation, sans partage entre sessions.
    static final class EtatSynchronisation {
        int x=Integer.MIN_VALUE, y=Integer.MIN_VALUE;
        String avatar=null;
        boolean gauche, mouvement;
        long dernierPing;
        int niveauMondeInitialise=Integer.MIN_VALUE;
    }
    private static volatile long derniereAlerteSync;
    static void signalerErreurSynchronisation(SQLException e) {
        long maintenant=System.currentTimeMillis();
        if(maintenant-derniereAlerteSync>10000L) {
            derniereAlerteSync=maintenant;
            System.err.println("Synchronisation multijoueur (nouvelle tentative): "+e.getMessage());
        }
    }

    // === MULTIJOUEUR === Publication de position à 100 ms, monde à ~300 ms.
    static void synchroniser(Connection c, InfosPartie infos, Jeu jeu, List<JoueurDistant> distants,
            EtatSynchronisation etat, boolean lireMonde) throws SQLException {
        Joueur j=jeu.getJoueur();
        int x=j.getPositionX(), y=j.getPositionY();
        String avatar=j.getNomAvatarActuel();
        boolean gauche=j.isRegardeVersLaGauche(), mouvement=j.estEnMouvement();
        long maintenant=System.currentTimeMillis();
        boolean changement=x!=etat.x || y!=etat.y || !avatar.equals(etat.avatar)
                || gauche!=etat.gauche || mouvement!=etat.mouvement;
        if(changement || maintenant-etat.dernierPing>=5000L) {
            String upd="UPDATE joueur_multi SET position_x=?,position_y=?,avatar_actuel=?,direction_gauche=?,en_mouvement=?,dernier_ping=CURRENT_TIMESTAMP(3) WHERE session_id=?";
            try(PreparedStatement p=c.prepareStatement(upd)) {
                p.setInt(1,x); p.setInt(2,y); p.setString(3,avatar); p.setBoolean(4,gauche); p.setBoolean(5,mouvement); p.setString(6,infos.getSessionId());
                if(p.executeUpdate()==0) throw new SQLException("Session multijoueur expirée.");
            }
            etat.x=x; etat.y=y; etat.avatar=avatar; etat.gauche=gauche; etat.mouvement=mouvement; etat.dernierPing=maintenant;
        }
        String sel="SELECT pseudo,avatar_actuel,position_x,position_y,direction_gauche,en_mouvement FROM joueur_multi WHERE code_partie=? AND session_id<>? AND dernier_ping >= CURRENT_TIMESTAMP(3) - INTERVAL 15 SECOND";
        List<JoueurDistant> nouveau=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement(sel)) {
            p.setString(1,infos.getIdentifiantPartie()); p.setString(2,infos.getSessionId());
            try(ResultSet r=p.executeQuery()) { while(r.next()) {
                String pseudo=r.getString(1), a=r.getString(2); int px=r.getInt(3),py=r.getInt(4);
                boolean dir=r.getBoolean(5), mouv=r.getBoolean(6);
                JoueurDistant precedent=null;
                synchronized(distants) { for(JoueurDistant d:distants) if(d.getPseudo().equals(pseudo)) {precedent=d;break;} }
                if(precedent==null) nouveau.add(new JoueurDistant(pseudo,a,px,py,dir,mouv));
                else {precedent.mettreAJourCible(px,py,a,dir,mouv);nouveau.add(precedent);}
            }}
        }
        synchronized(distants) { distants.clear(); distants.addAll(nouveau); }
        if(infos.estMultijoueur()) {
            if(infos.estHebergeur() && etat.niveauMondeInitialise!=jeu.getNumeroNiveau()) {
                initialiserMonde(c,infos,jeu);
                etat.niveauMondeInitialise=jeu.getNumeroNiveau();
            }
            publierEvenements(c,infos,jeu);
            if(lireMonde) synchroniserMonde(c,infos,jeu);
        }
    }

    // === MULTIJOUEUR === Retrait du seul jeton de session, sans effacer les autres.
    static void quitter(InfosPartie infos) {
        if(infos==null || infos.getSessionId()==null) return;
        try(Connection c=ConnexionBDD.getInstance().getConnection(); PreparedStatement p=c.prepareStatement("DELETE FROM joueur_multi WHERE session_id=?")) {
            p.setString(1,infos.getSessionId()); p.executeUpdate();
        } catch(SQLException e) { signalerErreurSynchronisation(e); }
    }

    // === MULTIJOUEUR === Les joueurs distants emploient les images locales identiques aux avatars.
    static void dessinerDistants(Graphics2D g,Jeu jeu,List<JoueurDistant> liste){
      List<JoueurDistant> copie; synchronized(liste){copie=new ArrayList<>(liste);} int h=jeu.getCarte().getHauteurPixels();
      for(JoueurDistant d:copie)d.dessinerAvecPseudo(g,d.getX(),h-d.getY()-d.getHitbox()[1]);
    }
}
