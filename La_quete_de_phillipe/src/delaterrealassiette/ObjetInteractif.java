package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Représente un élément du décor avec lequel un joueur agit.
 * [DEPENDANCE] Les objets interactifs partagent la géométrie et le rendu d’ElementDeJeu.
 * ________________________________________________________________ */
public abstract class ObjetInteractif extends ElementDeJeu {
    protected final Map map;
    protected boolean collisionJoueur;

    /* ________________________________________________________________
     * ACTION : Initialise un objet interactif sans dépendance à la carte.
     * ________________________________________________________________ */
    protected ObjetInteractif(int positionX, int positionY) {
        super(positionX, positionY);
        map = null;
    }

    /* ________________________________________________________________
     * ACTION : Initialise un objet interactif rattaché à une carte.
     * [DEPENDANCE] Map fournit les dimensions et le rendu des tuiles.
     * ________________________________________________________________ */
    protected ObjetInteractif(Map map, int positionX, int positionY) {
        super(positionX, positionY);
        this.map = map;
    }

    /* ________________________________________________________________
     * ACTION : Définit si l’objet bloque les entités mobiles.
     * ________________________________________________________________ */
    public void definirCollisionJoueur(boolean collisionJoueur) {
        this.collisionJoueur = collisionJoueur;
    }

    /* ________________________________________________________________
     * ACTION : Indique si l’objet bloque les entités mobiles.
     * ________________________________________________________________ */
    public boolean isCollisionJoueur() {
        return collisionJoueur;
    }

    /* ________________________________________________________________
     * ACTION : Applique l'interaction du joueur avec cet objet.
     * ________________________________________________________________ */
    public void interagir(Joueur joueur) {
    }

    /* ________________________________________________________________
     * ACTION : Retourne la taille de tuile pour le rendu dépendant de la carte.
     * ________________________________________________________________ */
    @Override
    protected int getTailleTuileDessin() {
        return map.getTailleTuile();
    }
}
