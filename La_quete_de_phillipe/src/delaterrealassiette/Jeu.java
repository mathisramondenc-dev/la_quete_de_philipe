package delaterrealassiette;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/* ________________________________________________________________
 * ACTION : Regroupe la map et les entités de la partie, les met à jour et les dessine.
 * [DEPENDANCE] FenetreDeJeu appelle miseAJour et rendu à chaque tic.
 * ________________________________________________________________ */
public class Jeu {
    private static final int DEPART_X = 32;
    private static final int DEPART_Y = 128;

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
     * ACTION : Dessine le décor puis chaque entité.
     * [INFO] Convertit l'ordonnée du monde en coordonnée d'écran.
     * ________________________________________________________________ */
    public void rendu(Graphics2D contexte) {
        carte.rendu(contexte);

        for (Entite entite : entites) {
            int[] hitbox = entite.getHitbox();
            int xEntite = entite.getPositionX();
            int yEntite = carte.getHauteurPixels()
                    - entite.getPositionY() - hitbox[1];
            entite.dessiner(contexte, xEntite, yEntite);
        }
    }
}