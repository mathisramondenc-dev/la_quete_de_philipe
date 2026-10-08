package jeu.entites;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

/** Base commune des objets visibles du jeu. */
public abstract class Entite {
    protected int x, y, vitesse;
    protected Image sprite;

    protected Entite(int x, int y, int vitesse, Image sprite) {
        this.x=x; this.y=y; this.vitesse=vitesse;
        this.sprite=sprite != null ? sprite : spriteParDefaut(Color.GRAY);
    }
    protected static Image spriteParDefaut(Color couleur) {
        BufferedImage image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics(); g.setColor(couleur); g.fillRect(0,0,16,16); g.dispose(); return image;
    }
    public abstract void miseAJour();
    /** Dessine le sprite en conservant sa position. */
    public void rendu(Graphics2D g) { g.drawImage(sprite,x,y,null); }
    public int getX(){return x;} public void setX(int x){this.x=x;}
    public int getY(){return y;} public void setY(int y){this.y=y;}
    public int getVitesse(){return vitesse;} public void setVitesse(int vitesse){this.vitesse=vitesse;}
    public Image getSprite(){return sprite;} public void setSprite(Image sprite){this.sprite=sprite;}
}
