package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Représente un ingrédient récupérable dans le niveau.
 * [DEPENDANCE] Jeu détecte le chevauchement et ajoute le type à la recette.
 * ________________________________________________________________ */
public class Ingredient extends ObjetInteractif {
    public static final int TAILLE_HITBOX = 24;
    private final TypeIngredient typeIngredient;

    public Ingredient(TypeIngredient typeIngredient, int positionX, int positionY) {
        super(positionX, positionY);
        this.typeIngredient = typeIngredient;
        hitbox = new int[]{TAILLE_HITBOX, TAILLE_HITBOX};
        cheminImage = typeIngredient.getCheminSprite();
    }

    /* ________________________________________________________________
     * ACTION : Retourne le type de cet ingrédient.
     * ________________________________________________________________ */
    public TypeIngredient getTypeIngredient() { return typeIngredient; }

    /* ________________________________________________________________
     * ACTION : Retourne la taille carrée de la hitbox.
     * ________________________________________________________________ */
    @Override public int[] getHitbox() { return hitbox; }

    /* ________________________________________________________________
     * ACTION : Retourne le chemin du sprite de cet ingrédient.
     * ________________________________________________________________ */
    @Override public String getCheminImage() { return cheminImage; }

    /* ________________________________________________________________
     * ACTION : Rend inactif l'ingrédient après interaction du joueur.
     * ________________________________________________________________ */
    @Override
    public void interagir(Joueur joueur) {
        estActif = false;
    }
}
