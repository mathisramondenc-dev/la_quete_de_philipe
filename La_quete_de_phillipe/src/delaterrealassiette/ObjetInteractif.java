package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Représente un élément du décor avec lequel un joueur agit.
 * [DEPENDANCE] Ingredient spécialise cette classe pour le ramassage.
 * ________________________________________________________________ */
public abstract class ObjetInteractif extends ElementDeJeu {
    protected ObjetInteractif(int positionX, int positionY) {
        super(positionX, positionY);
    }

    /* ________________________________________________________________
     * ACTION : Applique l'interaction du joueur avec cet objet.
     * ________________________________________________________________ */
    public abstract void interagir(Joueur joueur);
}
