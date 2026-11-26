package delaterrealassiette;

/**
 * Échelle du niveau.
 *
 * Elle ne bloque pas : on la traverse librement. Ce sont la Map et Entite
 * qui lui donnent son comportement : dans l'échelle, les touches haut/bas
 * font monter ou descendre sans gravité, et son sommet sert de sol sur
 * lequel on peut marcher. Le sprite est répété sur chaque tuile.
 */
public class Echelle extends ObjetInteractif {
    /** Position et dimensions en pixels. */
    public Echelle(Map map, int positionX, int positionY, int largeur, int hauteur) {
        super(map, positionX, positionY);
        hitbox = new int[]{largeur, hauteur};
        cheminImage = "images/echelle.png";
    }

    @Override
    protected boolean estImageRepeteParTuile() {
        return true;
    }

    @Override
    protected int getTailleTuileDessin() {
        return map.getTailleTuile();
    }
}
