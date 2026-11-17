package delaterrealassiette;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Crée la base La Quête de Philippe, son schéma et les trois avatars jouables. */
public class CreationBDD {
    private static final String BASE = parametre("laquete.db.name", "LAQUETE_DB_NAME", "2026-2027_s1_vs1_tp2_la_quete_de_philippe");

    private CreationBDD() {
    }

    public static void main(String[] args) {
        // === MULTIJOUEUR === La configuration JDBC est partagée avec les clients.
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
            // CREATE TABLE IF NOT EXISTS ne met pas à niveau les tables existantes.
            // Ces ALTER idempotents réparent notamment joueur_multi après une ancienne création.
            for (String ddl : migrationsMulti()) {
                commande.executeUpdate(ddl);
            }
            insererAvatars(connexion);
            insererCarteInitiale(connexion);
            System.out.println("Base " + BASE + " créée/vérifiée; tables et avatars prêts.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
            // === MULTIJOUEUR === nouvelles tables ajoutées sans modifier le schéma historique.
            "CREATE TABLE IF NOT EXISTS partie_multi ("
            + "code_partie VARCHAR(40) NOT NULL PRIMARY KEY, mot_de_passe VARCHAR(255) NOT NULL, "
            + "cree_le TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS joueur_multi ("
            + "session_id CHAR(36) NOT NULL PRIMARY KEY, code_partie VARCHAR(40) NOT NULL, "
            + "pseudo VARCHAR(15) NOT NULL, position_x INT NOT NULL DEFAULT 32, position_y INT NOT NULL DEFAULT 64, "
            + "avatar_actuel VARCHAR(40) NOT NULL DEFAULT 'Etchebest', direction_gauche TINYINT(1) NOT NULL DEFAULT 0, "
            + "en_mouvement TINYINT(1) NOT NULL DEFAULT 0, dernier_ping TIMESTAMP(3) NOT NULL, "
            + "UNIQUE KEY uq_partie_pseudo(code_partie,pseudo), KEY ix_joueur_presence(code_partie,dernier_ping), "
            + "CONSTRAINT fk_multijoueur_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            // === MULTIJOUEUR === état partagé par salle et niveau, clés stables par ordre dans la carte.
            "CREATE TABLE IF NOT EXISTS ingredient_multi ("
            + "code_partie VARCHAR(40) NOT NULL,niveau INT NOT NULL,ingredient_id INT NOT NULL,x INT NOT NULL,y INT NOT NULL,"
            + "type VARCHAR(40) NOT NULL,ramasse TINYINT(1) NOT NULL DEFAULT 0,ramasse_par VARCHAR(15) NULL,PRIMARY KEY(code_partie,niveau,ingredient_id),"
            + "FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS monstre_multi ("
            + "code_partie VARCHAR(40) NOT NULL,niveau INT NOT NULL,monstre_id INT NOT NULL,type VARCHAR(80) NOT NULL,"
            + "x INT NOT NULL,y INT NOT NULL,pv INT NOT NULL DEFAULT 100,vivant TINYINT(1) NOT NULL DEFAULT 1,"
            + "direction_gauche TINYINT(1) NOT NULL DEFAULT 0,degats_demandes INT NOT NULL DEFAULT 0,"
            + "PRIMARY KEY(code_partie,niveau,monstre_id),FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS levier_multi ("
            + "code_partie VARCHAR(40) NOT NULL,niveau INT NOT NULL,levier_id INT NOT NULL,id_mecanisme INT NOT NULL,"
            + "actif TINYINT(1) NOT NULL DEFAULT 0,PRIMARY KEY(code_partie,niveau,levier_id),"
            + "FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE"
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
     * Mise à niveau additive des colonnes non structurelles du schéma multijoueur.
     * ADD COLUMN IF NOT EXISTS est pris en charge par MariaDB et permet une exécution
     * à chaque démarrage sans supprimer les données. Tous les champs sont ajoutés
     * de façon additive; une table ancienne sans clés ne verra pas ses index recréés.
     */
    private static String[] migrationsMulti() {
        return new String[] {
            "ALTER TABLE partie_multi ADD COLUMN IF NOT EXISTS code_partie VARCHAR(40) NOT NULL DEFAULT ''",
            "ALTER TABLE partie_multi ADD COLUMN IF NOT EXISTS mot_de_passe VARCHAR(255) NOT NULL DEFAULT ''",
            "ALTER TABLE partie_multi ADD COLUMN IF NOT EXISTS cree_le TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP",

            "ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS session_id CHAR(36) NOT NULL DEFAULT ''",
            "ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS code_partie VARCHAR(40) NOT NULL DEFAULT ''",
            "ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS pseudo VARCHAR(15) NOT NULL DEFAULT ''",
            "ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS position_x INT NOT NULL DEFAULT 32",
            "ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS position_y INT NOT NULL DEFAULT 64",
            "ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS avatar_actuel VARCHAR(40) NOT NULL DEFAULT 'Etchebest'",
            "ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS direction_gauche TINYINT(1) NOT NULL DEFAULT 0",
            "ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS en_mouvement TINYINT(1) NOT NULL DEFAULT 0",
            "ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS dernier_ping TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)",
            // Index couvrant les consultations par partie et session; CREATE INDEX IF NOT EXISTS est idempotent sous MariaDB.
            "CREATE INDEX IF NOT EXISTS ix_joueur_multi_code_session ON joueur_multi(code_partie,session_id)",

            "ALTER TABLE ingredient_multi ADD COLUMN IF NOT EXISTS code_partie VARCHAR(40) NOT NULL DEFAULT ''",
            "ALTER TABLE ingredient_multi ADD COLUMN IF NOT EXISTS niveau INT NOT NULL DEFAULT 0",
            "ALTER TABLE ingredient_multi ADD COLUMN IF NOT EXISTS ingredient_id INT NOT NULL DEFAULT 0",
            "ALTER TABLE ingredient_multi ADD COLUMN IF NOT EXISTS x INT NOT NULL DEFAULT 0",
            "ALTER TABLE ingredient_multi ADD COLUMN IF NOT EXISTS y INT NOT NULL DEFAULT 0",
            "ALTER TABLE ingredient_multi ADD COLUMN IF NOT EXISTS type VARCHAR(40) NOT NULL DEFAULT ''",
            "ALTER TABLE ingredient_multi ADD COLUMN IF NOT EXISTS ramasse TINYINT(1) NOT NULL DEFAULT 0",
            "ALTER TABLE ingredient_multi ADD COLUMN IF NOT EXISTS ramasse_par VARCHAR(15) NULL",
            "UPDATE ingredient_multi SET ramasse=1 WHERE ramasse_par IS NOT NULL AND ramasse=0",

            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS code_partie VARCHAR(40) NOT NULL DEFAULT ''",
            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS niveau INT NOT NULL DEFAULT 0",
            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS monstre_id INT NOT NULL DEFAULT 0",
            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS type VARCHAR(80) NOT NULL DEFAULT ''",
            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS x INT NOT NULL DEFAULT 0",
            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS y INT NOT NULL DEFAULT 0",
            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS pv INT NOT NULL DEFAULT 100",
            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS vivant TINYINT(1) NOT NULL DEFAULT 1",
            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS direction_gauche TINYINT(1) NOT NULL DEFAULT 0",
            "ALTER TABLE monstre_multi ADD COLUMN IF NOT EXISTS degats_demandes INT NOT NULL DEFAULT 0",

            "ALTER TABLE levier_multi ADD COLUMN IF NOT EXISTS code_partie VARCHAR(40) NOT NULL DEFAULT ''",
            "ALTER TABLE levier_multi ADD COLUMN IF NOT EXISTS niveau INT NOT NULL DEFAULT 0",
            "ALTER TABLE levier_multi ADD COLUMN IF NOT EXISTS levier_id INT NOT NULL DEFAULT 0",
            "ALTER TABLE levier_multi ADD COLUMN IF NOT EXISTS id_mecanisme INT NOT NULL DEFAULT 0",
            "ALTER TABLE levier_multi ADD COLUMN IF NOT EXISTS actif TINYINT(1) NOT NULL DEFAULT 0"
        };
    }

    private static void insererAvatars(Connection connexion) throws SQLException {
        String sql = "INSERT INTO avatar (nom, vitesse, force_saut, hitbox_largeur, hitbox_hauteur, chemin_image) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE vitesse=VALUES(vitesse), "
                + "force_saut=VALUES(force_saut), hitbox_largeur=VALUES(hitbox_largeur), "
                + "hitbox_hauteur=VALUES(hitbox_hauteur), chemin_image=VALUES(chemin_image)";
        String[][] avatars = {
            {"Etchebest", "5", "12", "32", "64", "/sprites/etchebest.png"},
            {"Lignac", "7", "15", "28", "60", "/sprites/lignac.png"},
            {"Tarayre", "6", "10", "30", "58", "/sprites/tarayre.png"}
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

    private static void insererCarteInitiale(Connection connexion) throws SQLException {
        String sql = "INSERT INTO carte (carte_id, nom) VALUES (1, 'carte_principale') "
                + "ON DUPLICATE KEY UPDATE nom=VALUES(nom)";
        try (Statement commande = connexion.createStatement()) {
            commande.executeUpdate(sql);
        }
    }

    private static String parametre(String propriete, String variable, String valeurDefaut) {
        String valeur = System.getProperty(propriete);
        if (valeur == null || valeur.isEmpty()) {
            valeur = System.getenv(variable);
        }
        return (valeur == null || valeur.isEmpty()) ? valeurDefaut : valeur;
    }
}
