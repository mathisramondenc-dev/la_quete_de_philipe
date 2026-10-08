package jeu.bdd;
import java.sql.Connection; import java.sql.DriverManager; import java.sql.SQLException;
/** Connexion JDBC optionnelle: l'absence de MySQL laisse le jeu hors ligne. */
public final class SingletonJDBC { private static SingletonJDBC instance; private Connection connexion;
 private static final String URL="jdbc:mysql://localhost:3306/delaterrealassiette"; private static final String USER="root"; private static final String MDP="";
 private SingletonJDBC(){try{connexion=DriverManager.getConnection(URL,USER,MDP);}catch(SQLException e){connexion=null;}}
 public static synchronized SingletonJDBC getInstance(){if(instance==null)instance=new SingletonJDBC();return instance;}
 public Connection getConnexion(){return connexion;} public void fermer(){if(connexion!=null)try{connexion.close();}catch(SQLException e){/* hors ligne */}}
}
