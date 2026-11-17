package delaterrealassiette;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
/** Joueur distant interpolé entre les instantanés reçus de la BDD. */
public final class JoueurDistant extends ElementDeJeu {
 private final String pseudo; private volatile String avatar; private volatile boolean mouvement;
 private volatile int cibleX,cibleY;
 public JoueurDistant(String p,String a,int x,int y,boolean gauche,boolean m){super(x,y);pseudo=p;avatar=a;cibleX=x;cibleY=y;mouvement=m;configurerAvatar(a);definirOrientationGauche(gauche);}
 private void configurerAvatar(String a){avatar=a;if("Lignac".equals(a)){hitbox=new int[]{28,60};cheminImage="images/sprites/lignac.png";}else if("Tarayre".equals(a)){hitbox=new int[]{18,30};cheminImage="images/sprites/tarayre.png";}else{hitbox=new int[]{32,64};cheminImage="images/sprites/etchebest.png";}}
 public synchronized void mettreAJourCible(int x,int y,String nouvelAvatar,boolean gauche,boolean m){if(!avatar.equals(nouvelAvatar))configurerAvatar(nouvelAvatar);cibleX=x;cibleY=y;mouvement=m;definirOrientationGauche(gauche);}
 /** Avance d'une fraction du décalage à chaque frame, sans bloquer le rendu. */
 public void interpoler(){positionX=avancer(positionX,cibleX);positionY=avancer(positionY,cibleY);}
 private int avancer(int actuel,int cible){int ecart=cible-actuel;if(Math.abs(ecart)<=1)return cible;int pas=Math.max(1,Math.min(12,Math.abs(ecart)/4));return actuel+(ecart>0?pas:-pas);}
 public String getPseudo(){return pseudo;} public String getAvatar(){return avatar;} public int getX(){return positionX;} public int getY(){return positionY;} public int[] getHitbox(){return hitbox;} public String getCheminImage(){return cheminImage;} public boolean isEnMouvement(){return mouvement;}
 public void dessinerAvecPseudo(Graphics2D g,int x,int y){dessiner(g,x,y);g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(Color.WHITE);g.drawString(pseudo,x,y-5);}
}
