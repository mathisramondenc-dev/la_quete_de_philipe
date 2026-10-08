package jeu.map;
/** TODO: boite de collision rectangulaire. */
public class Hitbox { private int x,y,l,h; public Hitbox(int x,int y,int l,int h){this.x=x;this.y=y;this.l=l;this.h=h;} public boolean intersecte(Hitbox autre){return autre!=null&&x<autre.x+autre.l&&x+l>autre.x&&y<autre.y+autre.h&&y+h>autre.y;} public int getX(){return x;} public int getY(){return y;} public int getLargeur(){return l;} public int getHauteur(){return h;} }
