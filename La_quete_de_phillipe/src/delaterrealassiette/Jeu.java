package delaterrealassiette;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* ________________________________________________________________
 * ACTION : Gère la carte, le joueur, les ingrédients et la progression.
 * [DEPENDANCE] FenetreDeJeu appelle miseAJour et rendu à chaque tic.
 * ________________________________________________________________ */
public class Jeu {
    private static final int DEPART_X = 32;
    private static final int DEPART_Y = 64;

    private String fichierNiveau;
    private int numeroNiveau;
    private Map carte;
    private Joueur joueur;
    private List<Entite> entites;
    private List<Ingredient> ingredients;
    private InventaireRecette inventaireRecette;
    private boolean victoire;

    /* ________________________________________________________________
     * ACTION : Charge un niveau choisi et initialise sa recette.
     * [INFO] Le niveau choisi devient le point de départ de la progression.
     * ________________________________________________________________ */
    public Jeu(String fichierNiveau) {
        this.fichierNiveau = fichierNiveau;
        numeroNiveau = extraireNumeroNiveau(fichierNiveau);
        chargerNiveau(null);
    }

    /* ________________________________________________________________
     * ACTION : Charge la carte, la recette et le joueur de départ.
     * ________________________________________________________________ */
    private void chargerNiveau(Joueur joueurPrecedent) {
        carte = new Map(fichierNiveau);
        joueur = joueurPrecedent == null
                ? new Joueur(carte, "joueurtesthitbox")
                : new Joueur(carte, joueurPrecedent);
        joueur.definirPosition(DEPART_X, DEPART_Y);
        entites = new ArrayList<>();
        entites.add(joueur);
        entites.addAll(carte.getLeviers());
        entites.addAll(carte.getBlocsPorte());
        entites.addAll(carte.getBlocsMortels());
        ingredients = new ArrayList<>(carte.getIngredients());
        inventaireRecette = new InventaireRecette(ingredients);
    }

    /* ________________________________________________________________
     * ACTION : Extrait le numéro du niveau depuis son nom de fichier.
     * ________________________________________________________________ */
    private int extraireNumeroNiveau(String nomFichier) {
        String nom = new java.io.File(nomFichier).getName();
        return Integer.parseInt(nom.replace("niveau", "").replace(".txt", ""));
    }

    /* ________________________________________________________________
     * ACTION : Met à jour le joueur, ramasse les ingrédients et progresse.
     * [INFO] Une seule transition est effectuée après la collecte complète.
     * ________________________________________________________________ */
    public void miseAJour() {
        miseAJour(new boolean[9]);
    }

    /* ________________________________________________________________
     * ACTION : Met à jour le joueur, les mécanismes, les entités et la recette.
     * ________________________________________________________________ */
    public void miseAJour(boolean[] etatBoutons) {
        if (victoire) {
            return;
        }
        joueur.miseAJourIntentions(etatBoutons);
        if (joueur.consommerActivationLevier()) {
            for (Levier levier : carte.getLeviers()) {
                if (levier.estAProximite(joueur)) {
                    levier.basculer();
                    break;
                }
            }
        }
        carte.mettreAJourMecanismes(entites);
        carte.miseAJour();
        for (Entite entite : entites) {
            entite.mettreAJour();
        }
        if (estTombeDansLeVide() || estEnContactAvecBlocMortel()) {
            joueur.modifierPointsDeVie(-joueur.getPointsDeVie());
        }
        if (joueur.estMort()) {
            reset();
            return;
        }
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isActif()) {
                continue;
            }
            for (Entite entite : entites) {
                if (entite instanceof Joueur
                        && chevauchement(ingredient, (Joueur) entite)) {
                    ingredient.interagir((Joueur) entite);
                    inventaireRecette.ajouter(ingredient.getTypeIngredient());
                    break;
                }
            }
        }
        if (inventaireRecette.estComplete()) {
            passerAuNiveauSuivant();
        }
    }

    /* ________________________________________________________________
     * ACTION : Indique si le joueur est entièrement passé sous la carte.
     * [INFO] y = 0 correspond au bas du décor, le vide est donc en y négatif.
     * ________________________________________________________________ */
    private boolean estTombeDansLeVide() {
        int hauteurHitbox = joueur.getHitbox()[1];
        return joueur.getPositionY() + hauteurHitbox < 0;
    }

    /* ________________________________________________________________
     * ACTION : Indique si le joueur touche un bloc mortel.
     * ________________________________________________________________ */
    private boolean estEnContactAvecBlocMortel() {
        for (BlocMortel blocMortel : carte.getBlocsMortels()) {
            if (blocMortel.enCollisionAvec(joueur)) {
                return true;
            }
        }
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Relance le niveau actuel comme au premier lancement.
     * [INFO] Carte, ingrédients, inventaire et joueur sont recréés à neuf.
     * ________________________________________________________________ */
    private void reset() {
        chargerNiveau(null);
    }

    /* ________________________________________________________________
     * ACTION : Compare les hitbox d’un ingrédient et du joueur.
     * ________________________________________________________________ */
    private boolean chevauchement(Ingredient ingredient, Joueur joueur) {
        int[] hitboxIngredient = ingredient.getHitbox();
        int[] hitboxJoueur = joueur.getHitbox();
        return ingredient.getPositionX() < joueur.getPositionX() + hitboxJoueur[0]
                && ingredient.getPositionX() + hitboxIngredient[0] > joueur.getPositionX()
                && ingredient.getPositionY() < joueur.getPositionY() + hitboxJoueur[1]
                && ingredient.getPositionY() + hitboxIngredient[1] > joueur.getPositionY();
    }

    /* ________________________________________________________________
     * ACTION : Charge le niveau suivant ou verrouille l’état de victoire.
     * [INFO] Le nouvel inventaire est initialisé une seule fois avec la carte.
     * ________________________________________________________________ */
    private void passerAuNiveauSuivant() {
        if (numeroNiveau >= 3) {
            victoire = true;
            return;
        }
        numeroNiveau++;
        fichierNiveau = "niveau" + numeroNiveau + ".txt";
        Joueur joueurPrecedent = joueur;
        chargerNiveau(joueurPrecedent);
    }

    /* ________________________________________________________________
     * ACTION : Dessine le décor puis les entités du niveau actif.
     * ________________________________________________________________ */
    public void rendu(Graphics2D contexte) {
        carte.rendu(contexte);
        for (Entite entite : entites) {
            int[] hitbox = entite.getHitbox();
            int xEntite = entite.getPositionX();
            int yEntite = carte.getHauteurPixels() - entite.getPositionY() - hitbox[1];
            entite.dessiner(contexte, xEntite, yEntite);
        }
    }

    /* ________________________________________________________________
     * ACTION : Retourne la carte du niveau actif.
     * ________________________________________________________________ */
    public Map getCarte() {
        return carte;
    }

    /* ________________________________________________________________
     * ACTION : Retourne les ingrédients du niveau actif.
     * ________________________________________________________________ */
    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    /* ________________________________________________________________
     * ACTION : Retourne l’inventaire actif.
     * ________________________________________________________________ */
    public InventaireRecette getInventaireRecette() {
        return inventaireRecette;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hauteur de la carte en pixels.
     * ________________________________________________________________ */
    public int getHauteurCartePixels() {
        return carte.getHauteurPixels();
    }

    /* ________________________________________________________________
     * ACTION : Retourne le joueur contrôlé.
     * ________________________________________________________________ */
    public Joueur getJoueur() {
        return joueur;
    }

    /* ________________________________________________________________
     * ACTION : Dessine les ingrédients encore actifs.
     * ________________________________________________________________ */
    public void dessinerIngredients(Graphics2D contexte) {
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isActif()) {
                continue;
            }
            int yEcran = carte.getHauteurPixels() - ingredient.getPositionY()
                    - ingredient.getHitbox()[1];
            ingredient.dessiner(contexte, ingredient.getPositionX(), yEcran);
        }
    }

    /* ________________________________________________________________
     * ACTION : Indique si le dernier niveau est terminé.
     * ________________________________________________________________ */
    public boolean isVictoire() {
        return victoire;
    }

    /* ________________________________________________________________
     * ACTION : Retourne l’index du niveau actuellement actif.
     * ________________________________________________________________ */
    public int getNumeroNiveau() {
        return numeroNiveau;
    }
}
