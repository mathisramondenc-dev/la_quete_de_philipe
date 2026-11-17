package delaterrealassiette;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Configuration JDBC centralisée (Singleton), selon le patron présenté au cours. */
public final class ConnexionBDD {
    private static final ConnexionBDD INSTANCE = new ConnexionBDD();
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
        return DriverManager.getConnection("jdbc:mariadb://" + h + ":" + p + "/" + b, u, m);
    }
}
