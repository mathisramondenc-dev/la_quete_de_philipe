package delaterrealassiette;

/**
 * Liste des ingrédients du jeu, chacun avec son nom affiché et son sprite.
 *
 * Le nom de la constante (TOMATE, PAIN...) est celui écrit dans la section
 * INGREDIENTS des fichiers de niveau ; ajouter un ingrédient = ajouter une
 * ligne ici et son image dans images/ingredients/.
 */
public enum TypeIngredient {
    TOMATE("Tomate", "images/ingredients/tomate.png"),
    FROMAGE("Fromage", "images/ingredients/fromage.png"),
    OIGNON("Oignon", "images/ingredients/oignon.png"),
    SALADE("Salade", "images/ingredients/salade.png"),
    PAIN("Pain", "images/ingredients/pain.png"),
    CHAMPIGNON("Champignon", "images/ingredients/champignon.png"),
    POMME_DE_TERRE("Pomme de terre", "images/ingredients/pomme_de_terre.png"),
    CAROTTE("Carotte", "images/ingredients/carotte.png"),
    OEUF("Œuf", "images/ingredients/oeuf.png"),
    BEURRE("Beurre", "images/ingredients/beurre.png"),
    LAIT("Lait", "images/ingredients/lait.png"),
    FARINE("Farine", "images/ingredients/farine.png"),
    JAMBON("Jambon", "images/ingredients/jambon.png"),
    BOEUF("Bœuf", "images/ingredients/boeuf.png"),
    AIL("Ail", "images/ingredients/ail.png");

    private final String nom;
    private final String cheminSprite;

    TypeIngredient(String nom, String cheminSprite) {
        this.nom = nom;
        this.cheminSprite = cheminSprite;
    }

    /** @return nom en français, avec accents, pour l'affichage */
    public String getNom() { return nom; }

    public String getCheminSprite() { return cheminSprite; }
}
