package delaterrealassiette;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Point d'accès unique à la base MariaDB (patron Singleton vu en cours).
 *
 * Toutes les classes obtiennent leurs connexions ici, ce qui centralise les
 * paramètres de connexion. Chaque paramètre se lit, dans l'ordre : propriété
 * système (-Dlaquete.db.host=...), variable d'environnement (LAQUETE_DB_HOST),
 * sinon valeur par défaut (serveur de l'école).
 *
 * Chaque appel ouvre une nouvelle connexion, que l'appelant ferme lui-même
 * (try-with-resources). Au premier appel, les tables multijoueur sont créées
 * si besoin et mises à niveau si elles viennent d'une ancienne version du
 * jeu, pour qu'un poste à jour fonctionne même sur une base plus ancienne.
 */
public final class ConnexionBDD {
    private static final ConnexionBDD INSTANCE = new ConnexionBDD();
    /** Vrai une fois le schéma vérifié : la vérification n'est faite qu'une fois par lancement. */
    private static volatile boolean schemaMultijoueurPret;
    private ConnexionBDD() { }
    public static ConnexionBDD getInstance() { return INSTANCE; }

    /** @return propriété système, sinon variable d'environnement, sinon valeur par défaut */
    private String parametre(String propriete, String variable, String defaut) {
        String v = System.getProperty(propriete);
        if (v == null || v.isEmpty()) v = System.getenv(variable);
        return v == null || v.isEmpty() ? defaut : v;
    }

    /**
     * Ouvre une connexion et s'assure que le schéma multijoueur est prêt.
     *
     * @return connexion neuve, à fermer par l'appelant
     * @throws SQLException si la base est injoignable ou le schéma impossible à préparer
     */
    public Connection getConnection() throws SQLException {
        String h = parametre("laquete.db.host", "LAQUETE_DB_HOST", "nemrod.ens2m.fr");
        String p = parametre("laquete.db.port", "LAQUETE_DB_PORT", "3306");
        String b = parametre("laquete.db.name", "LAQUETE_DB_NAME", "2026-2027_s1_vs1_tp2_la_quete_de_philippe");
        String u = parametre("laquete.db.user", "LAQUETE_DB_USER", "etudiant");
        String m = parametre("laquete.db.password", "LAQUETE_DB_PASSWORD", "YTDTvj9TR3CDYCmP");
        Connection c = DriverManager.getConnection("jdbc:mariadb://" + h + ":" + p + "/" + b, u, m);
        try {
            assurerSchemaMultijoueur(c);
            return c;
        } catch (SQLException e) {
            try { c.close(); } catch (SQLException fermeture) { e.addSuppressed(fermeture); }
            throw new SQLException("Connexion ouverte, mais échec de vérification/migration du schéma multijoueur : "
                    + e.getMessage() + " (SQLState=" + e.getSQLState()
                    + ", code=" + e.getErrorCode() + ")", e.getSQLState(), e.getErrorCode(), e);
        }
    }

    /**
     * Crée les tables multijoueur absentes puis les met à niveau :
     * - partie_multi : une ligne par partie (code, mot de passe, niveau atteint) ;
     * - joueur_multi : une ligne par joueur connecté (position, chef, dernier signe de vie) ;
     * - levier_multi / ingredient_multi : état partagé des leviers et ingrédients
     *   ramassés, par partie et par niveau.
     * Les deux dernières ne contiennent que des états temporaires : si leur
     * format est trop ancien, elles sont simplement recréées.
     */
    private static synchronized void assurerSchemaMultijoueur(Connection c) throws SQLException {
        if (schemaMultijoueurPret) return;
        try (java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS partie_multi (code_partie VARCHAR(40) NOT NULL PRIMARY KEY, mot_de_passe VARCHAR(255) NOT NULL, cree_le TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, niveau_courant INT NOT NULL DEFAULT 1) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS joueur_multi (session_id CHAR(36) NOT NULL PRIMARY KEY, code_partie VARCHAR(40) NOT NULL, pseudo VARCHAR(15) NOT NULL, position_x INT NOT NULL DEFAULT 32, position_y INT NOT NULL DEFAULT 64, avatar_actuel VARCHAR(40) NOT NULL DEFAULT 'Etchebest', dernier_ping TIMESTAMP(3) NOT NULL, UNIQUE KEY uq_partie_pseudo(code_partie,pseudo), KEY ix_joueur_presence(code_partie,dernier_ping), CONSTRAINT fk_multijoueur_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS levier_multi (code_partie VARCHAR(40) NOT NULL, niveau_id INT NOT NULL, levier_id INT NOT NULL, actif BOOLEAN NOT NULL, PRIMARY KEY(code_partie,niveau_id,levier_id), CONSTRAINT fk_levier_multi_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS ingredient_multi (code_partie VARCHAR(40) NOT NULL, niveau_id INT NOT NULL, ingredient_id VARCHAR(100) NOT NULL, PRIMARY KEY(code_partie,niveau_id,ingredient_id), CONSTRAINT fk_ingredient_multi_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }

        // Ancien format sans niveau_id : CREATE IF NOT EXISTS ne le corrige pas, on recrée
        if (!colonneExiste(c, "levier_multi", "niveau_id")
                || !colonneExiste(c, "ingredient_multi", "niveau_id")) {
            System.err.println("[BDD] Schéma multijoueur ancien détecté : recréation de levier_multi et ingredient_multi (états temporaires supprimés).");
            try (java.sql.Statement s = c.createStatement()) {
                s.executeUpdate("DROP TABLE IF EXISTS levier_multi");
                s.executeUpdate("DROP TABLE IF EXISTS ingredient_multi");
                s.executeUpdate("CREATE TABLE levier_multi (code_partie VARCHAR(40) NOT NULL, niveau_id INT NOT NULL, levier_id INT NOT NULL, actif BOOLEAN NOT NULL, PRIMARY KEY(code_partie,niveau_id,levier_id), CONSTRAINT fk_levier_multi_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                s.executeUpdate("CREATE TABLE ingredient_multi (code_partie VARCHAR(40) NOT NULL, niveau_id INT NOT NULL, ingredient_id VARCHAR(100) NOT NULL, PRIMARY KEY(code_partie,niveau_id,ingredient_id), CONSTRAINT fk_ingredient_multi_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            } catch (SQLException e) {
                throw erreurSchema("migration de levier_multi/ingredient_multi", e);
            }
        }

        // Colonnes ajoutées après coup : ajout sans perte des parties et joueurs existants
        ajouterColonneSiAbsente(c, "partie_multi", "niveau_courant",
                "ALTER TABLE partie_multi ADD COLUMN niveau_courant INT NOT NULL DEFAULT 1");
        ajouterColonneSiAbsente(c, "joueur_multi", "avatar_actuel",
                "ALTER TABLE joueur_multi ADD COLUMN avatar_actuel VARCHAR(40) NOT NULL DEFAULT 'Etchebest'");
        schemaMultijoueurPret = true;
    }

    /**
     * Exécute l'ALTER TABLE donné si la colonne manque. Un échec est ignoré
     * si la colonne existe finalement (un autre joueur l'a ajoutée au même moment).
     */
    private static void ajouterColonneSiAbsente(Connection c, String table, String colonne,
                                                  String sql) throws SQLException {
        if (colonneExiste(c, table, colonne)) return;
        try (java.sql.Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        } catch (SQLException e) {
            if (!colonneExiste(c, table, colonne)) throw erreurSchema("ajout de " + table + "." + colonne, e);
        }
    }

    /** Interroge le catalogue INFORMATION_SCHEMA de la base courante. */
    private static boolean colonneExiste(Connection c, String table, String colonne) throws SQLException {
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=? LIMIT 1";
        try (java.sql.PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, table);
            p.setString(2, colonne);
            try (java.sql.ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    /** Emballe une erreur SQL avec le nom de l'opération pour un message plus clair. */
    private static SQLException erreurSchema(String operation, SQLException cause) {
        return new SQLException("Échec de la " + operation + " du schéma multijoueur : "
                + cause.getMessage() + " (SQLState=" + cause.getSQLState()
                + ", code=" + cause.getErrorCode() + ")", cause.getSQLState(), cause.getErrorCode(), cause);
    }

}
