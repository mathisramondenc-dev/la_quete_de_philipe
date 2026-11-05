package delaterrealassiette;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Accès JDBC MariaDB et synchronisation facultative des objets du jeu. */
public class GestionBDD {
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
}
