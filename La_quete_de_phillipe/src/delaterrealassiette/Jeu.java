// Jeu.java
package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/* ________________________________________________________________
 * ACTION : Regroupe la map et les entités de la partie, les met à jour
 *          et les dessine à chaque tic.
 * [INFO] Pour l'instant, une entité est dessinée par sa hitbox seule.
 * [DEPENDANCE] FenetreDeJeu appelle miseAJour et rendu à chaque tic.
 * ________________________________________________________________ */
public class Jeu {
    // Position de départ du joueur, posé sur le sol de deux tuiles de haut
    private static final int DEPART_X = 32;
    private static final int DEPART_Y = 64;

    private Map carte;
    private Joueur joueur;
    private List<Entite> entites;
    private GestionBDD gestionBDD;

    /* ________________________________________________________________
     * ACTION : Charge le niveau et place le joueur à son point de départ.
     * [INFO] Toutes les entités partagent cette unique instance de Map.
     * ________________________________________________________________ */
    public Jeu(String fichierNiveau) {
        carte = new Map(fichierNiveau);

        joueur = new Joueur(carte, "joueurtesthitbox");
        Joueur joueur2 = new Joueur(carte, "joueurtesthitbox");
        joueur.definirPosition(DEPART_X, DEPART_Y);
        joueur2.definirPosition(DEPART_X, DEPART_Y);

        entites = new ArrayList<>();
        entites.add(joueur);
        entites.add(joueur2);
    }

    /* ________________________________________________________________
     * ACTION : Transmet les touches au joueur puis actualise la partie.
     * [DEPENDANCE] ClavierSouris fournit le tableau de booléens.
     * ________________________________________________________________ */
    public void miseAJour(boolean[] etatBoutons) {
        joueur.miseAJourIntentions(etatBoutons);
        carte.miseAJour();
        for (Entite entite : entites) {
            entite.mettreAJour();
        }
    }

    /* ________________________________________________________________
     * ACTION : Dessine le décor puis la hitbox de chaque entité.
     * [INFO] y monte pour les entités et descend à l'écran, d'où la conversion.
     * ________________________________________________________________ */
    public void rendu(Graphics2D contexte) {
        carte.rendu(contexte);
        
        
        //On choisit la couleur rouge pour dessiner les hitbox
        contexte.setColor(Color.RED);
        
        for (Entite entite : entites) {
            
            int[] hitbox = entite.getHitbox();
            
            //xHitbox c'est simplement la positionX de entite
            int xHitbox = entite.getPositionX();
            
            //Ici par contre c'est différent car on par du bas de la carte on reprends la même méthode que pour la collision :
            // 1) Bas de la carte
            int basCarte = carte.getHauteurPixels();
            // 2) Pieds de l'entité : on remonte de positionY depuis le bas de la carte
            int piedsHitbox = basCarte - entite.getPositionY();
            // 3) Haut de la hitbox : on remonte de sa hauteur, car drawRect attend le coin haut
            int yHitbox = piedsHitbox - hitbox[1];
            
            //4) On dessine le rectangle mais avec des -1 car c'est propre à la méthode : drawRect(x, y, w, h) dessine w+1 pixels de large et h+1 de haut
            contexte.drawRect(xHitbox, yHitbox, hitbox[0] - 1, hitbox[1] - 1);
        }
    }
}
