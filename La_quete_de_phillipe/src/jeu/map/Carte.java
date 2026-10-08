package jeu.map;
import java.awt.Color; import java.awt.Graphics2D;
/** Carte minimale composee de tuiles sans obstacle. */
public class Carte { public static final int TAILLE_TUILE=32; private final int largeur,hauteur;
 public Carte(int largeur,int hauteur){this.largeur=largeur;this.hauteur=hauteur;}
 public void rendu(Graphics2D g){g.setColor(new Color(235,220,180));g.fillRect(0,0,largeur,hauteur);g.setColor(new Color(210,190,145));for(int x=0;x<largeur;x+=TAILLE_TUILE)for(int y=0;y<hauteur;y+=TAILLE_TUILE)g.drawRect(x,y,TAILLE_TUILE,TAILLE_TUILE);}
 public int getYSol(){return hauteur-TAILLE_TUILE;} public int getLargeur(){return largeur;} public int getHauteur(){return hauteur;} }
