package jeu.entites.monstres.ia;
import jeu.entites.monstres.Monstre;
/** Contrat d'un etat de la machine a etats des monstres. */
public interface EtatMonstre { void entrer(Monstre monstre); void mettreAJour(Monstre monstre); void sortir(Monstre monstre); }
