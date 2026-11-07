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
    private List<Ingredient> ingredients;
    private InventaireRecette inventaireRecette;

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
        ingredients = new ArrayList<>(carte.getIngredients());
        inventaireRecette = new InventaireRecette(ingredients);
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
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isActif()) {
                continue;
            }
            for (Entite entite : entites) {
                if (entite instanceof Joueur && ingredient.enCollisionAvec(entite)) {
                    ingredient.interagir((Joueur) entite);
                    inventaireRecette.ajouter(ingredient.getTypeIngredient());
                    break;
                }
            }
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

    /* ________________________________________________________________
     * ACTION : Retourne les ingrédients présents dans le niveau.
     * ________________________________________________________________ */
    public List<Ingredient> getIngredients() { return java.util.Collections.unmodifiableList(ingredients); }

    /* ________________________________________________________________
     * ACTION : Retourne l'inventaire de recette partagé par la partie.
     * ________________________________________________________________ */
    public InventaireRecette getInventaireRecette() { return inventaireRecette; }

    /* ________________________________________________________________
     * ACTION : Retourne la hauteur du monde en pixels.
     * ________________________________________________________________ */
    public int getHauteurCartePixels() { return carte.getHauteurPixels(); }

    /* ________________________________________________________________
     * ACTION : Retourne le joueur contrôlé.
     * ________________________________________________________________ */
    public Joueur getJoueur() { return joueur; }

    /* ________________________________________________________________
     * ACTION : Dessine les ingrédients actifs par-dessus le décor et les entités.
     * ________________________________________________________________ */
    public void dessinerIngredients(Graphics2D contexte) {
        for (Ingredient ingredient : ingredients) {
            int yEcran = carte.getHauteurPixels() - ingredient.getPositionY() - ingredient.getHitbox()[1];
            ingredient.dessiner(contexte, ingredient.getPositionX(), yEcran);
        }
    }
}