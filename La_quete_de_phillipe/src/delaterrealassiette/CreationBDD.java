package delaterrealassiette;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Programme à part (il a son propre main) qui prépare la base de données :
 * création de toutes les tables, puis insertion des quatre chefs et de la
 * carte principale.
 *
 * Il est sans danger de le relancer : les tables ne sont créées que si elles
 * manquent (CREATE TABLE IF NOT EXISTS) et les insertions mettent à jour les
 * lignes existantes (ON DUPLICATE KEY UPDATE). Les paramètres de connexion
 * se lisent comme dans ConnexionBDD (propriété système, variable
 * d'environnement, valeur par défaut).
 *
 * Tables : avatar, carte, tuile, joueur et monstre pour les données du jeu ;
 * partie_multi, joueur_multi, levier_multi et ingredient_multi pour le
 * multijoueur.
 */
public class CreationBDD {
    private static final String BASE = parametre("laquete.db.name", "LAQUETE_DB_NAME", "2026-2027_s1_vs1_tp2_la_quete_de_philippe");

    /** Classe utilitaire : pas d'instance. */
    private CreationBDD() {
    }

    public static void main(String[] args) {
        String hote = parametre("laquete.db.host", "LAQUETE_DB_HOST", "nemrod.ens2m.fr");
        String port = parametre("laquete.db.port", "LAQUETE_DB_PORT", "3306");
        String utilisateur = parametre("laquete.db.user", "LAQUETE_DB_USER", "etudiant");
        String motDePasse = parametre("laquete.db.password", "LAQUETE_DB_PASSWORD", "YTDTvj9TR3CDYCmP");
        String urlBase = "jdbc:mariadb://" + hote + ":" + port + "/" + BASE;

        try (Connection connexion = DriverManager.getConnection(urlBase, utilisateur, motDePasse);
             Statement commande = connexion.createStatement()) {
            for (String ddl : definitions()) {
                commande.executeUpdate(ddl);
            }
            insererAvatars(connexion);
            insererCarteInitiale(connexion);
            System.out.println("Base " + BASE + " créée/vérifiée; tables et avatars prêts.");
        } catch (SQLException e) {
            System.err.println("Échec de création/vérification du schéma MariaDB : "
                    + e.getMessage() + " (SQLState=" + e.getSQLState()
                    + ", code=" + e.getErrorCode() + ")");
            e.printStackTrace();
        }
    }

    /**
     * @return les requêtes de création des tables, dans un ordre qui respecte
     *         les clés étrangères (une table référencée est créée avant)
     */
    private static String[] definitions() {
        return new String[] {
            "CREATE TABLE IF NOT EXISTS avatar ("
            + "nom VARCHAR(40) NOT NULL PRIMARY KEY, vitesse INT NOT NULL, force_saut INT NOT NULL, "
            + "hitbox_largeur INT NOT NULL, hitbox_hauteur INT NOT NULL, chemin_image VARCHAR(255) NOT NULL"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS carte ("
            + "carte_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(100) NOT NULL UNIQUE, "
            + "largeur INT NOT NULL DEFAULT 0, hauteur INT NOT NULL DEFAULT 0, taille_tuile INT NOT NULL DEFAULT 32"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS joueur ("
            + "pseudo VARCHAR(80) NOT NULL PRIMARY KEY, points_de_vie INT NOT NULL DEFAULT 100, "
            + "points_de_vie_max INT NOT NULL DEFAULT 100, score INT NOT NULL DEFAULT 0, "
            + "position_x INT NOT NULL DEFAULT 0, position_y INT NOT NULL DEFAULT 0, "
            + "avatar_actuel VARCHAR(40) NOT NULL, "
            + "CONSTRAINT fk_joueur_avatar FOREIGN KEY (avatar_actuel) REFERENCES avatar(nom) "
            + "ON UPDATE CASCADE ON DELETE RESTRICT"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS monstre ("
            + "monstre_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, type_monstre VARCHAR(80) NOT NULL, "
            + "position_x INT NOT NULL DEFAULT 0, position_y INT NOT NULL DEFAULT 0, vitesse INT NOT NULL, "
            + "degats INT NOT NULL, hitbox_largeur INT NOT NULL, hitbox_hauteur INT NOT NULL, "
            + "chemin_image VARCHAR(255) NOT NULL"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            // Multijoueur (mêmes définitions que dans ConnexionBDD)
            "CREATE TABLE IF NOT EXISTS partie_multi ("
            + "code_partie VARCHAR(40) NOT NULL PRIMARY KEY, mot_de_passe VARCHAR(255) NOT NULL, "
            + "cree_le TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, niveau_courant INT NOT NULL DEFAULT 1"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS joueur_multi ("
            + "session_id CHAR(36) NOT NULL PRIMARY KEY, code_partie VARCHAR(40) NOT NULL, "
            + "pseudo VARCHAR(15) NOT NULL, position_x INT NOT NULL DEFAULT 32, position_y INT NOT NULL DEFAULT 64, "
            + "avatar_actuel VARCHAR(40) NOT NULL DEFAULT 'Etchebest', dernier_ping TIMESTAMP(3) NOT NULL, "
            + "UNIQUE KEY uq_partie_pseudo(code_partie,pseudo), KEY ix_joueur_presence(code_partie,dernier_ping), "
            + "CONSTRAINT fk_multijoueur_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS levier_multi ("
            + "code_partie VARCHAR(40) NOT NULL, niveau_id INT NOT NULL, levier_id INT NOT NULL, actif BOOLEAN NOT NULL, "
            + "PRIMARY KEY(code_partie,niveau_id,levier_id), "
            + "CONSTRAINT fk_levier_multi_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS ingredient_multi ("
            + "code_partie VARCHAR(40) NOT NULL, niveau_id INT NOT NULL, ingredient_id VARCHAR(100) NOT NULL, "
            + "PRIMARY KEY(code_partie,niveau_id,ingredient_id), "
            + "CONSTRAINT fk_ingredient_multi_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS tuile ("
            + "carte_id INT NOT NULL, indice_ligne INT NOT NULL, indice_colonne INT NOT NULL, numero_tuile INT NOT NULL, "
            + "PRIMARY KEY (carte_id, indice_ligne, indice_colonne), "
            + "CONSTRAINT fk_tuile_carte FOREIGN KEY (carte_id) REFERENCES carte(carte_id) "
            + "ON UPDATE CASCADE ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        };
    }

    /**
     * Insère ou met à jour les quatre chefs. Colonnes : nom, vitesse, force
     * de saut, largeur et hauteur de hitbox, sprite. Les valeurs réellement
     * utilisées en jeu sont celles codées dans Joueur.
     */
    private static void insererAvatars(Connection connexion) throws SQLException {
        String sql = "INSERT INTO avatar (nom, vitesse, force_saut, hitbox_largeur, hitbox_hauteur, chemin_image) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE vitesse=VALUES(vitesse), "
                + "force_saut=VALUES(force_saut), hitbox_largeur=VALUES(hitbox_largeur), "
                + "hitbox_hauteur=VALUES(hitbox_hauteur), chemin_image=VALUES(chemin_image)";
        String[][] avatars = {
            {"Etchebest", "5", "12", "32", "64", "/sprites/etchebest.png"},
            {"Lignac", "7", "15", "28", "60", "/sprites/lignac.png"},
            {"Tarayre", "6", "10", "30", "58", "/sprites/tarayre.png"},
            {"Maite", "6", "10", "32", "64", "/sprites/maite.png"}
        };
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            for (String[] avatar : avatars) {
                requete.setString(1, avatar[0]);
                requete.setInt(2, Integer.parseInt(avatar[1]));
                requete.setInt(3, Integer.parseInt(avatar[2]));
                requete.setInt(4, Integer.parseInt(avatar[3]));
                requete.setInt(5, Integer.parseInt(avatar[4]));
                requete.setString(6, avatar[5]);
                requete.addBatch();
            }
            requete.executeBatch();
        }
    }

    /** Crée la carte n°1, utilisée par la sauvegarde des tuiles de GestionBDD. */
    private static void insererCarteInitiale(Connection connexion) throws SQLException {
        String sql = "INSERT INTO carte (carte_id, nom) VALUES (1, 'carte_principale') "
                + "ON DUPLICATE KEY UPDATE nom=VALUES(nom)";
        try (Statement commande = connexion.createStatement()) {
            commande.executeUpdate(sql);
        }
    }

    /** @return propriété système, sinon variable d'environnement, sinon valeur par défaut */
    private static String parametre(String propriete, String variable, String valeurDefaut) {
        String valeur = System.getProperty(propriete);
        if (valeur == null || valeur.isEmpty()) {
            valeur = System.getenv(variable);
        }
        return (valeur == null || valeur.isEmpty()) ? valeurDefaut : valeur;
    }
}
