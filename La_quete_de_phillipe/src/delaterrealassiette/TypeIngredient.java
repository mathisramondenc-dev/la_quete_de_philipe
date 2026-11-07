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
    CHAMPIGNON("Champignon", "images/ingredients/champignon.png");

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
