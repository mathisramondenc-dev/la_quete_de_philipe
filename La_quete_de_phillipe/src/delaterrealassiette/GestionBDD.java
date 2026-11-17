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
            return new InfosPartie(pseudo, code, motDePasse, id);
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

    // === MULTIJOUEUR === Chaque instance du jeu possède un thread JDBC dédié.
    static void synchroniser(Connection c, InfosPartie infos, Jeu jeu, List<JoueurDistant> distants) throws SQLException {
        Joueur j=jeu.getJoueur();
        String upd="UPDATE joueur_multi SET position_x=?,position_y=?,avatar_actuel=?,dernier_ping=CURRENT_TIMESTAMP(3) WHERE session_id=?";
        try(PreparedStatement p=c.prepareStatement(upd)) {
            p.setInt(1,j.getPositionX()); p.setInt(2,j.getPositionY()); p.setString(3,j.getNomAvatarActuel()); p.setString(4,infos.getSessionId());
            if(p.executeUpdate()==0) throw new SQLException("Session multijoueur expirée.");
        }
        List<JoueurDistant> nouveau=new ArrayList<>();
        String sel="SELECT pseudo,avatar_actuel,position_x,position_y FROM joueur_multi WHERE code_partie=? AND session_id<>? AND dernier_ping >= CURRENT_TIMESTAMP(3) - INTERVAL 15 SECOND";
        try(PreparedStatement p=c.prepareStatement(sel)) {
            p.setString(1,infos.getIdentifiantPartie()); p.setString(2,infos.getSessionId());
            try(ResultSet r=p.executeQuery()){while(r.next()) nouveau.add(new JoueurDistant(r.getString(1),r.getString(2),r.getInt(3),r.getInt(4)));}
        }
        synchronized(distants){distants.clear();distants.addAll(nouveau);}
    }

    // === MULTIJOUEUR === Retrait du seul jeton de session, sans effacer les autres.
    static void quitter(InfosPartie infos) {
        if(infos==null || infos.getSessionId()==null) return;
        try(Connection c=ConnexionBDD.getInstance().getConnection(); PreparedStatement p=c.prepareStatement("DELETE FROM joueur_multi WHERE session_id=?")) {
            p.setString(1,infos.getSessionId()); p.executeUpdate();
        } catch(SQLException e) { System.err.println("Déconnexion BDD différée; expiration automatique active: "+e.getMessage()); }
    }

    static void dessinerDistants(Graphics2D g, Jeu jeu, List<JoueurDistant> liste) {
        List<JoueurDistant> copie;
        synchronized(liste){copie=new ArrayList<>(liste);}
        Map m=jeu.getCarte();
        for(JoueurDistant d:copie) {
            int x=d.getX(), y=m.getHauteurPixels()-d.getY()-40;
            Color col="Lignac".equals(d.getAvatar())?new Color(70,190,255):("Tarayre".equals(d.getAvatar())?new Color(255,180,60):new Color(250,90,140));
            g.setColor(Color.BLACK);g.fillOval(x-2,y-2,36,44);g.setColor(col);g.fillOval(x,y,32,40);
            g.setColor(Color.WHITE);g.drawString(d.getPseudo(),x-4,y-6);
        }
    }
}
