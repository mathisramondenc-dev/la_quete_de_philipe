package jeu.entites.monstres;

import java.awt.Color;
import java.awt.Image;
import jeu.entites.Entite;

/** Classe de base des monstres synchronises. */
public abstract class Monstre extends Entite {
    protected int id;
    protected Monstre(int id,int x,int y,int vitesse,Image sprite){super(x,y,vitesse,sprite!=null?sprite:spriteParDefaut(Color.GREEN));this.id=id;}
    public int getId(){return id;}
}
