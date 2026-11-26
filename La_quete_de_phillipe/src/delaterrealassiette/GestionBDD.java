package delaterrealassiette;

import java.awt.Graphics2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Toutes les requêtes SQL du jeu. Les connexions viennent de ConnexionBDD.
 *
 * La classe a deux parties indépendantes :
 *
 * 1) Sauvegarde solo (méthodes d'instance) : enregistre et recharge l'état
 *    d'un joueur (table joueur) et les tuiles modifiées de la carte (table
 *    tuile). Remarque : cette partie n'est appelée nulle part pour l'instant.
 *
 * 2) Multijoueur (méthodes static) :
 *    - heberger() / rejoindre() créent la partie ou y inscrivent le joueur
 *      sous un identifiant de session unique (UUID) ;
 *    - synchroniser(), appelée en boucle par SynchronisationBDD, publie la
 *      position du joueur et ses actions (leviers, ingrédients), puis lit les
 *      autres joueurs et l'état partagé qu'elle transmet au Jeu ;
 *    - quitter() supprime la session à la fermeture.
 *    Un joueur est considéré en ligne s'il a donné signe de vie
 *    (dernier_ping) depuis moins de 15 secondes : un jeu qui plante
 *    disparaît donc tout seul de la partie.
 */
public class GestionBDD {
    /** Traces de synchronisation, activées avec -Dlaquete.sync.debug=true. */
    private static final boolean LOG_SYNC = Boolean.getBoolean("laquete.sync.debug");
    /** Identifiant de la carte sauvegardée (créée par CreationBDD). */
    private static final int ID_CARTE = 1;
    private Connection connexion;
    private Joueur joueur;
    private Map carte;
    private String pseudo;

    // ===================== 1) Sauvegarde solo =====================

    /** Indique quel joueur et quelle carte importer / exporter. */
    public void configurer(Joueur joueur, String pseudo, Map carte) {
        this.joueur = joueur;
        this.pseudo = pseudo;
        this.carte = carte;
    }

    /** Ouvre la connexion utilisée par la sauvegarde ; en cas d'échec, l'erreur est affichée en console. */
    public void demarrer() {
        try {
            connexion = ConnexionBDD.getInstance().getConnection();
        } catch (SQLException e) {
            System.err.println("Impossible d'ouvrir ou de préparer la connexion MariaDB : "
                    + e.getMessage() + " (SQLState=" + e.getSQLState()
                    + ", code=" + e.getErrorCode() + ")");
            e.printStackTrace();
        }
    }

    /** Non utilisée : la lecture des paramètres est faite par ConnexionBDD. */
    private String parametre(String propriete, String variable, String valeurDefaut) {
        String valeur = System.getProperty(propriete);
        if (valeur == null || valeur.isEmpty()) {
            valeur = System.getenv(variable);
        }
        return (valeur == null || valeur.isEmpty()) ? valeurDefaut : valeur;
    }

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

    /**
     * Recharge depuis la base les PV, le score, la position et le chef du
     * joueur, puis les tuiles enregistrées de la carte. Le maximum de PV reste
     * celui défini dans Joueur, même si la base indique autre chose.
     */
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
        } else if ("Maite".equals(nom)) {
            joueur.changerChef(joueur.getMaite());
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

    /**
     * Enregistre le joueur, et la carte seulement si elle a changé, dans une
     * seule transaction : soit tout est enregistré, soit rien (rollback).
     */
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

    /** Insère le joueur, ou met à jour sa ligne s'il existe déjà (même pseudo). */
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

    /** Enregistre les dimensions de la carte puis toutes ses tuiles, envoyées en un seul lot (batch). */
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

    // ===================== 2) Multijoueur =====================

    /**
     * Crée une partie et y inscrit l'hébergeur, en une seule transaction.
     * Les leviers et ingrédients d'une ancienne partie au même code sont effacés.
     *
     * @return informations de connexion, avec un nouvel identifiant de session
     * @throws SQLException si le code est déjà utilisé ou la base injoignable
     */
    public static InfosPartie heberger(String pseudo, String code, String motDePasse) throws SQLException {
        String id = UUID.randomUUID().toString();
        try (Connection c = ConnexionBDD.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO partie_multi(code_partie,mot_de_passe) VALUES (?,?)")) {
                    p.setString(1, code);
                    p.setString(2, motDePasse);
                    p.executeUpdate();
                }
                reinitialiserEtatsMultijoueur(c, code);
                inscrire(c, id, code, pseudo);
                c.commit();
                return new InfosPartie(pseudo, code, motDePasse, id);
            } catch (SQLException e) {
                try {
                    c.rollback();
                } catch (SQLException erreurRollback) {
                    e.addSuppressed(erreurRollback);
                }
                throw e;
            }
        }
    }

    /** Vide l'état des leviers et ingrédients de la partie (les noms de tables sont fixes, le code est paramétré). */
    private static void reinitialiserEtatsMultijoueur(Connection c, String code) throws SQLException {
        String[] tables = {"levier_multi", "ingredient_multi"};
        for (String table : tables) {
            try (PreparedStatement p = c.prepareStatement(
                    "DELETE FROM " + table + " WHERE code_partie=?")) {
                p.setString(1, code);
                p.executeUpdate();
            }
        }
    }

    /**
     * Vérifie que la partie existe avec ce mot de passe, puis inscrit le joueur.
     *
     * @throws SQLException si la partie ou le mot de passe est faux, ou si le
     *                      pseudo est déjà pris dans cette partie
     */
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

    /** Ajoute la ligne du joueur dans joueur_multi (Etchebest, position de départ par défaut). */
    private static void inscrire(Connection c, String id, String code, String pseudo) throws SQLException {
        String sql = "INSERT INTO joueur_multi(session_id,code_partie,pseudo,position_x,position_y,avatar_actuel,dernier_ping) VALUES (?,?,?,32,64,'Etchebest',CURRENT_TIMESTAMP(3))";
        try (PreparedStatement p = c.prepareStatement(sql)) { p.setString(1,id); p.setString(2,code); p.setString(3,pseudo); p.executeUpdate(); }
    }

    /** @return pseudos des joueurs en ligne dans la partie, par ordre alphabétique */
    public static List<String> listerJoueurs(String code) throws SQLException {
        List<String> resultat = new ArrayList<>();
        String sql = "SELECT pseudo FROM joueur_multi WHERE code_partie=? AND dernier_ping >= CURRENT_TIMESTAMP(3) - INTERVAL 15 SECOND ORDER BY pseudo";
        try (Connection c=ConnexionBDD.getInstance().getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,code); try(ResultSet r=p.executeQuery()){ while(r.next()) resultat.add(r.getString(1)); }
        }
        return Collections.unmodifiableList(resultat);
    }

    /**
     * Un cycle de synchronisation (appelé par le thread de SynchronisationBDD) :
     * 1) publie la position et le chef du joueur, ce qui renouvelle aussi son
     *    signe de vie (une session supprimée provoque une erreur) ;
     * 2) lit les autres joueurs en ligne et remplace la liste partagée ;
     * 3) synchronise les leviers, ingrédients et le niveau (synchroniserObjets).
     *
     * @param distants liste lue par l'affichage, modifiée sous synchronized
     */
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
        synchroniserObjets(c, infos, jeu);
    }

    /**
     * Échange de l'état du monde avec la base, en trois temps :
     * 1) écriture : les actions locales en attente dans le Jeu (leviers,
     *    ingrédients ramassés) sont publiées dans une transaction, puis
     *    confirmées au Jeu une fois validées ;
     * 2) niveau : la base garde le plus haut niveau atteint (GREATEST), si
     *    bien qu'un joueur qui arrive au niveau 1 ne fait pas reculer la partie ;
     * 3) lecture : leviers et ingrédients du niveau partagé, transmis au Jeu
     *    qui les appliquera sur son propre thread.
     */
    private static void synchroniserObjets(Connection c, InfosPartie infos, Jeu jeu) throws SQLException {
        boolean autoCommit = c.getAutoCommit();
        java.util.Map<Integer, java.util.Map<Integer, Boolean>> leviersConfirmes = new HashMap<>();
        java.util.Map<Integer, Set<String>> ingredientsConfirmes = new HashMap<>();
        // 1) Écriture des actions locales
        try {
            c.setAutoCommit(false);
            for (Integer niveauModifie : jeu.getNiveauxModifies()) {
                java.util.Map<Integer, Boolean> leviersLocaux = jeu.getLeviersModifies(niveauModifie);
                Set<String> ingredientsLocaux = jeu.getIngredientsModifies(niveauModifie);
                String sqlLevier = "INSERT INTO levier_multi(code_partie,niveau_id,levier_id,actif) VALUES (?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE actif=VALUES(actif)";
                try (PreparedStatement p = c.prepareStatement(sqlLevier)) {
                    for (java.util.Map.Entry<Integer, Boolean> entree : leviersLocaux.entrySet()) {
                        p.setString(1, infos.getIdentifiantPartie());
                        p.setInt(2, niveauModifie);
                        p.setInt(3, entree.getKey());
                        p.setBoolean(4, entree.getValue());
                        int resultat = p.executeUpdate();
                        if (LOG_SYNC) System.out.println("[SYNC] Écriture levier id=" + entree.getKey()
                                + ", niveau_id=" + niveauModifie + ", code_partie="
                                + infos.getIdentifiantPartie() + ", résultat executeUpdate=" + resultat);
                    }
                }
                // INSERT IGNORE : un ingrédient déjà ramassé par un autre joueur n'est pas une erreur
                String sqlIngredient = "INSERT IGNORE INTO ingredient_multi(code_partie,niveau_id,ingredient_id) VALUES (?,?,?)";
                try (PreparedStatement p = c.prepareStatement(sqlIngredient)) {
                    for (String identifiant : ingredientsLocaux) {
                        p.setString(1, infos.getIdentifiantPartie());
                        p.setInt(2, niveauModifie);
                        p.setString(3, identifiant);
                        int resultat = p.executeUpdate();
                        if (LOG_SYNC) System.out.println("[SYNC] Écriture ingrédient id=" + identifiant
                                + ", niveau_id=" + niveauModifie + ", code_partie="
                                + infos.getIdentifiantPartie() + ", résultat executeUpdate=" + resultat);
                    }
                }
                leviersConfirmes.put(niveauModifie, leviersLocaux);
                ingredientsConfirmes.put(niveauModifie, ingredientsLocaux);
            }
            c.commit();
        } catch (SQLException e) {
            try { c.rollback(); } catch (SQLException erreurRollback) { e.addSuppressed(erreurRollback); }
            throw e;
        } finally {
            c.setAutoCommit(autoCommit);
        }
        // Confirmation seulement après le commit : rien n'est perdu en cas d'échec
        for (Integer niveauModifie : leviersConfirmes.keySet()) {
            jeu.confirmerChangementsMultijoueur(niveauModifie,
                    leviersConfirmes.get(niveauModifie), ingredientsConfirmes.get(niveauModifie));
        }

        // 2) Niveau partagé : ne peut qu'augmenter
        int niveauLocal = jeu.getNumeroNiveau();
        String majNiveau = "UPDATE partie_multi SET niveau_courant=GREATEST(niveau_courant,?) WHERE code_partie=?";
        try (PreparedStatement p = c.prepareStatement(majNiveau)) {
            p.setInt(1, niveauLocal);
            p.setString(2, infos.getIdentifiantPartie());
            int resultat = p.executeUpdate();
            if (LOG_SYNC) System.out.println("[SYNC] Publication niveau local=" + niveauLocal
                    + ", code_partie=" + infos.getIdentifiantPartie() + ", résultat executeUpdate=" + resultat);
        }
        int niveauPartage;
        try (PreparedStatement p = c.prepareStatement(
                "SELECT niveau_courant FROM partie_multi WHERE code_partie=?")) {
            p.setString(1, infos.getIdentifiantPartie());
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("Partie multijoueur introuvable: " + infos.getIdentifiantPartie());
                niveauPartage = r.getInt("niveau_courant");
            }
        }
        if (niveauPartage < 1 || niveauPartage > 3) {
            throw new SQLException("Niveau multijoueur invalide: " + niveauPartage);
        }

        // 3) Lecture de l'état partagé du niveau
        int nbLeviers = 0;
        java.util.Map<Integer, Boolean> leviers = new HashMap<>();
        String selLevier = "SELECT levier_id,actif FROM levier_multi WHERE code_partie=? AND niveau_id=?";
        try (PreparedStatement p = c.prepareStatement(selLevier)) {
            p.setString(1, infos.getIdentifiantPartie());
            p.setInt(2, niveauPartage);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) { leviers.put(r.getInt("levier_id"), r.getBoolean("actif")); nbLeviers++; }
            }
        }
        int nbIngredients = 0;
        Set<String> ingredients = new HashSet<>();
        String selIngredient = "SELECT ingredient_id FROM ingredient_multi WHERE code_partie=? AND niveau_id=?";
        try (PreparedStatement p = c.prepareStatement(selIngredient)) {
            p.setString(1, infos.getIdentifiantPartie());
            p.setInt(2, niveauPartage);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) { ingredients.add(r.getString("ingredient_id")); nbIngredients++; }
            }
        }
        if (LOG_SYNC) System.out.println("[SYNC] Lecture code_partie=" + infos.getIdentifiantPartie()
                + ", niveau_id=" + niveauPartage + ": leviers=" + nbLeviers
                + ", ingrédients=" + nbIngredients);
        jeu.appliquerEtatMultijoueur(niveauPartage, leviers, ingredients);
    }

    /**
     * Supprime uniquement la session de ce joueur. Si la base est injoignable,
     * la session disparaîtra d'elle-même après 15 s sans signe de vie.
     */
    static void quitter(InfosPartie infos) {
        if(infos==null || infos.getSessionId()==null) return;
        try(Connection c=ConnexionBDD.getInstance().getConnection(); PreparedStatement p=c.prepareStatement("DELETE FROM joueur_multi WHERE session_id=?")) {
            p.setString(1,infos.getSessionId()); p.executeUpdate();
        } catch(SQLException e) { System.err.println("Déconnexion BDD différée; expiration automatique active: "+e.getMessage()); }
    }

    /**
     * Dessine les autres joueurs avec leur pseudo au-dessus. On travaille sur
     * une copie de la liste pour ne pas bloquer le thread de synchronisation
     * pendant le dessin. Sans sprite, un rectangle de couleur par chef.
     */
    static void dessinerDistants(Graphics2D g, Jeu jeu, List<JoueurDistant> liste) {
        List<JoueurDistant> copie;
        synchronized(liste){copie=new ArrayList<>(liste);}
        Map m=jeu.getCarte();
        for (JoueurDistant d : copie) {
            int x = d.getX(), y = m.getHauteurPixels() - d.getY() - d.getHauteur();
            java.awt.image.BufferedImage image = ElementDeJeu.chargerImageDepuisCache(d.getCheminImage());
            if (image != null) g.drawImage(image, x, y, d.getLargeur(), d.getHauteur(), null);
            else {
                Color couleur = "Lignac".equals(d.getAvatar()) ? new Color(70, 190, 255)
                        : ("Tarayre".equals(d.getAvatar()) ? new Color(255, 180, 60)
                        : ("Maite".equals(d.getAvatar()) ? new Color(180, 110, 220) : new Color(250, 90, 140)));
                g.setColor(couleur); g.fillRect(x, y, d.getLargeur(), d.getHauteur());
            }
            g.setColor(Color.WHITE); g.drawString(d.getPseudo(), x - 4, y - 6);
        }
    }
}
