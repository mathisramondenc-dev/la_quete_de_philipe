package jeu.bdd;
import java.sql.*; import java.util.*; import jeu.entites.Avatar;
/** Acces aux avatars avec requetes preparees et mode hors ligne. */
public class AvatarDAO { public void inscrire(Avatar a){if(a==null)return;Connection c=SingletonJDBC.getInstance().getConnexion();if(c==null)return;try(PreparedStatement p=c.prepareStatement("INSERT INTO joueur(x,y) VALUES (?,?)")){p.setInt(1,a.getX());p.setInt(2,a.getY());p.executeUpdate();}catch(SQLException e){/* le jeu continue */}}
 public void envoyer(Avatar a){if(a==null)return;Connection c=SingletonJDBC.getInstance().getConnexion();if(c==null)return;try(PreparedStatement p=c.prepareStatement("UPDATE joueur SET x=?, y=? WHERE id=?")){p.setInt(1,a.getX());p.setInt(2,a.getY());p.setInt(3,a.getId());p.executeUpdate();}catch(SQLException e){}}
 public List<Avatar> lireTous(){List<Avatar> r=new ArrayList<Avatar>();Connection c=SingletonJDBC.getInstance().getConnexion();if(c==null)return r;try(PreparedStatement p=c.prepareStatement("SELECT id,x,y FROM joueur");ResultSet s=p.executeQuery()){while(s.next())r.add(new Avatar(s.getInt(1),s.getInt(2),s.getInt(3),false));}catch(SQLException e){}return r;}
 public void supprimer(int id){Connection c=SingletonJDBC.getInstance().getConnexion();if(c==null)return;try(PreparedStatement p=c.prepareStatement("DELETE FROM joueur WHERE id=?")){p.setInt(1,id);p.executeUpdate();}catch(SQLException e){}}
}
