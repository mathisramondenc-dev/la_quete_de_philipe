package delaterrealassiette;

/**
 * Ingrédient à ramasser dans le niveau.
 *
 * Ramasser = interagir() : l'ingrédient devient inactif, il n'est donc plus
 * dessiné ni ramassable. C'est la classe Jeu qui détecte le contact avec le
 * joueur et qui ajoute le type à l'inventaire.
 *
 * En multijoueur, chaque ingrédient est repéré par un identifiant construit
 * à partir de son type et de sa position, identique sur toutes les machines
 * puisqu'elles chargent le même fichier de niveau.
 */
public class Ingredient extends ObjetInteractif {
    public static final int TAILLE_HITBOX = 24;
    private final TypeIngredient typeIngredient;

    public Ingredient(TypeIngredient typeIngredient, int positionX, int positionY) {
        super(positionX, positionY);
        this.typeIngredient = typeIngredient;
        hitbox = new int[]{TAILLE_HITBOX, TAILLE_HITBOX};
        cheminImage = typeIngredient.getCheminSprite();
    }

    public TypeIngredient getTypeIngredient() { return typeIngredient; }

    /** @return identifiant de la forme "TYPE_x_y", le même pour tous les joueurs de la partie */
    public String getIdentifiantMultijoueur() {
        return typeIngredient.name() + "_" + getPositionX() + "_" + getPositionY();
    }

    /** Ramassage : l'ingrédient disparaît du niveau. */
    @Override
    public void interagir(Joueur joueur) {
        estActif = false;
    }
}
