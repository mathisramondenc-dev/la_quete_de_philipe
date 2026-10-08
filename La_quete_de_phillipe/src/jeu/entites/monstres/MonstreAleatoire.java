package jeu.entites.monstres;

import java.awt.Image;
import java.util.Random;

/** Monstre qui choisit periodiquement une direction aleatoire. */
public class MonstreAleatoire extends Monstre {
    private int dirX,dirY,compteur; private final Random hasard=new Random();
    private int largeur=640, hauteur=480;
    public MonstreAleatoire(int id,int x,int y,Image sprite){super(id,x,y,2,sprite); choisirDirection();}
    public MonstreAleatoire(int id,int x,int y){this(id,x,y,null);}
    private void choisirDirection(){dirX=hasard.nextInt(3)-1;dirY=hasard.nextInt(3)-1;compteur=30+hasard.nextInt(60);}
    public void miseAJour(){if(--compteur<=0)choisirDirection();x+=dirX*vitesse;y+=dirY*vitesse;x=Math.max(0,Math.min(largeur-16,x));y=Math.max(0,Math.min(hauteur-16,y));}
    public void setBornes(int largeur,int hauteur){this.largeur=Math.max(16,largeur);this.hauteur=Math.max(16,hauteur);}
    public int getDirX(){return dirX;} public int getDirY(){return dirY;} public int getCompteur(){return compteur;}
}
