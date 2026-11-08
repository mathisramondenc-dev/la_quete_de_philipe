package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Énumère les ingrédients et leurs noms ainsi que leurs sprites.
 * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Retourne le nom français de l’ingrédient.
     * ________________________________________________________________ */
    public String getNom() { return nom; }

    /* ________________________________________________________________
     * ACTION : Retourne le chemin de son image.
     * ________________________________________________________________ */
    public String getCheminSprite() { return cheminSprite; }
}
