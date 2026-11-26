package delaterrealassiette;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Configuration JDBC centralisée (Singleton), selon le patron présenté au cours. */
public final class ConnexionBDD {
    private static final ConnexionBDD INSTANCE = new ConnexionBDD();
    private static volatile boolean schemaMultijoueurPret;
    private ConnexionBDD() { }
    public static ConnexionBDD getInstance() { return INSTANCE; }

    private String parametre(String propriete, String variable, String defaut) {
        String v = System.getProperty(propriete);
        if (v == null || v.isEmpty()) v = System.getenv(variable);
        return v == null || v.isEmpty() ? defaut : v;
    }

    /** Ouvre une connexion à usage exclusif; le demandeur la ferme avec try-with-resources. */
    public Connection getConnection() throws SQLException {
        String h = parametre("laquete.db.host", "LAQUETE_DB_HOST", "nemrod.ens2m.fr");
        String p = parametre("laquete.db.port", "LAQUETE_DB_PORT", "3306");
        String b = parametre("laquete.db.name", "LAQUETE_DB_NAME", "2026-2027_s1_vs1_tp2_la_quete_de_philippe");
        String u = parametre("laquete.db.user", "LAQUETE_DB_USER", "etudiant");
        String m = parametre("laquete.db.password", "LAQUETE_DB_PASSWORD", "YTDTvj9TR3CDYCmP");
        Connection c = DriverManager.getConnection("jdbc:mariadb://" + h + ":" + p + "/" + b, u, m);
        try { assurerSchemaMultijoueur(c); return c; }
        catch (SQLException e) { c.close(); throw e; }
    }

    /** Crée automatiquement les tables requises par le jeu multijoueur. */
    private static synchronized void assurerSchemaMultijoueur(Connection c) throws SQLException {
        if (schemaMultijoueurPret) return;
        try (java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS partie_multi (code_partie VARCHAR(40) NOT NULL PRIMARY KEY, mot_de_passe VARCHAR(255) NOT NULL, cree_le TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, niveau_courant INT NOT NULL DEFAULT 1) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS joueur_multi (session_id CHAR(36) NOT NULL PRIMARY KEY, code_partie VARCHAR(40) NOT NULL, pseudo VARCHAR(15) NOT NULL, position_x INT NOT NULL DEFAULT 32, position_y INT NOT NULL DEFAULT 64, avatar_actuel VARCHAR(40) NOT NULL DEFAULT 'Etchebest', dernier_ping TIMESTAMP(3) NOT NULL, UNIQUE KEY uq_partie_pseudo(code_partie,pseudo), KEY ix_joueur_presence(code_partie,dernier_ping), CONSTRAINT fk_multijoueur_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS levier_multi (code_partie VARCHAR(40) NOT NULL, niveau_id INT NOT NULL, levier_id INT NOT NULL, actif BOOLEAN NOT NULL, PRIMARY KEY(code_partie,niveau_id,levier_id), CONSTRAINT fk_levier_multi_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS ingredient_multi (code_partie VARCHAR(40) NOT NULL, niveau_id INT NOT NULL, ingredient_id VARCHAR(100) NOT NULL, PRIMARY KEY(code_partie,niveau_id,ingredient_id), CONSTRAINT fk_ingredient_multi_partie FOREIGN KEY(code_partie) REFERENCES partie_multi(code_partie) ON DELETE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        // Migration sûre d'une partie_multi déjà créée avant le partage du niveau.
        if (!colonneNiveauExiste(c)) {
            try (java.sql.Statement s = c.createStatement()) {
                s.executeUpdate("ALTER TABLE partie_multi ADD COLUMN niveau_courant INT NOT NULL DEFAULT 1");
            } catch (SQLException e) {
                if (!colonneNiveauExiste(c)) throw e; // migration concurrente
            }
        }
        if (!colonneAvatarExiste(c)) {
            try (java.sql.Statement s = c.createStatement()) { s.executeUpdate("ALTER TABLE joueur_multi ADD COLUMN avatar_actuel VARCHAR(40) NOT NULL DEFAULT 'Etchebest'"); }
            catch (SQLException e) { if (!colonneAvatarExiste(c)) throw e; } // migration concurrente
        }
        schemaMultijoueurPret = true;
    }
    private static boolean colonneNiveauExiste(Connection c) throws SQLException {
        try (java.sql.ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, "partie_multi", "niveau_courant")) { if (r.next()) return true; }
        try (java.sql.ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, "PARTIE_MULTI", "NIVEAU_COURANT")) { return r.next(); }
    }
    private static boolean colonneAvatarExiste(Connection c) throws SQLException {
        try (java.sql.ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, "joueur_multi", "avatar_actuel")) { if (r.next()) return true; }
        try (java.sql.ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, "JOUEUR_MULTI", "AVATAR_ACTUEL")) { return r.next(); }
    }
}
