package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Représente un ingrédient récupérable dans le niveau.
 * [DEPENDANCE] Jeu détecte le chevauchement et ajoute le type à l’inventaire des ingrédients.
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
     * ACTION : Rend inactif l'ingrédient après interaction du joueur.
     * ________________________________________________________________ */
    @Override
    public void interagir(Joueur joueur) {
        estActif = false;
    }
}
