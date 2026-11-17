package delaterrealassiette;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
/** Instantané distant rendu avec le même pipeline de sprites qu'ElementDeJeu. */
// === MULTIJOUEUR ===
public final class JoueurDistant extends ElementDeJeu {
 private final String pseudo,avatar; private final boolean mouvement;
 public JoueurDistant(String p,String a,int x,int y,boolean gauche,boolean m){super(x,y);pseudo=p;avatar=a;mouvement=m;
  if("Lignac".equals(a)){hitbox=new int[]{28,60};cheminImage="images/sprites/lignac.png";}
  else if("Tarayre".equals(a)){hitbox=new int[]{18,30};cheminImage="images/sprites/tarayre.png";}
  else{hitbox=new int[]{32,64};cheminImage="images/sprites/etchebest.png";} definirOrientationGauche(gauche);}
 public String getPseudo(){return pseudo;} public String getAvatar(){return avatar;} public int getX(){return positionX;} public int getY(){return positionY;} public int[] getHitbox(){return hitbox;} public String getCheminImage(){return cheminImage;} public boolean isEnMouvement(){return mouvement;}
 public void dessinerAvecPseudo(Graphics2D g,int x,int y){dessiner(g,x,y);g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(Color.WHITE);g.drawString(pseudo,x,y-5);}
}
