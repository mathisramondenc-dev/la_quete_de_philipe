package jeu.bdd;
import java.sql.*; import java.util.*; import jeu.entites.monstres.*;
/** Synchronisation des monstres et election simple de l'hote. */
public class MonstreDAO { public void envoyer(List<Monstre> ms){Connection c=SingletonJDBC.getInstance().getConnexion();if(c==null)return;try{for(Monstre m:ms){try(PreparedStatement p=c.prepareStatement("UPDATE monstre SET x=?,y=? WHERE id=?")){p.setInt(1,m.getX());p.setInt(2,m.getY());p.setInt(3,m.getId());p.executeUpdate();}}}catch(SQLException e){}}
 public List<Monstre> lireTous(){List<Monstre> r=new ArrayList<Monstre>();Connection c=SingletonJDBC.getInstance().getConnexion();if(c==null)return r;try(PreparedStatement p=c.prepareStatement("SELECT id,x,y FROM monstre");ResultSet s=p.executeQuery()){while(s.next())r.add(new MonstreAleatoire(s.getInt(1),s.getInt(2),s.getInt(3)));}catch(SQLException e){}return r;}
 public boolean suisJeHote(){Connection c=SingletonJDBC.getInstance().getConnexion();if(c==null)return true;try(PreparedStatement p=c.prepareStatement("SELECT MIN(id) FROM joueur");ResultSet s=p.executeQuery()){return s.next();}catch(SQLException e){return true;}}
}
