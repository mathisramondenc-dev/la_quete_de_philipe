package delaterrealassiette;

/**
 * Élément fixe du niveau avec lequel le joueur peut interagir : ingrédient,
 * levier, porte, échelle, bloc mortel.
 *
 * Contrairement à une Entite, il n'est pas soumis à la physique. Il apporte
 * deux choses en plus d'ElementDeJeu :
 * - collisionJoueur : indique si l'objet bloque les entités (ex. porte fermée) ;
 * - interagir() : action déclenchée par le joueur, vide par défaut.
 *
 * La référence à la Map sert au rendu en tuiles ; elle vaut null pour les
 * objets qui n'en ont pas besoin (ingrédients).
 */
public abstract class ObjetInteractif extends ElementDeJeu {
    protected final Map map;
    protected boolean collisionJoueur;

    /** Objet indépendant de la carte (map vaut null). */
    protected ObjetInteractif(int positionX, int positionY) {
        super(positionX, positionY);
        map = null;
    }

    /** Objet dont le rendu dépend des tuiles de la carte. */
    protected ObjetInteractif(Map map, int positionX, int positionY) {
        super(positionX, positionY);
        this.map = map;
    }

    /** @param collisionJoueur true si l'objet doit bloquer les entités */
    public void definirCollisionJoueur(boolean collisionJoueur) {
        this.collisionJoueur = collisionJoueur;
    }

    public boolean isCollisionJoueur() {
        return collisionJoueur;
    }

    /** Effet d'une interaction du joueur ; ne fait rien par défaut. */
    public void interagir(Joueur joueur) {
    }

    @Override
    protected int getTailleTuileDessin() {
        return map.getTailleTuile();
    }
}
