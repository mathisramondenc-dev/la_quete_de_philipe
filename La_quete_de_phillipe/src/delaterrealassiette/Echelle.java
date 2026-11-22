package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Représente une échelle traversable et son sommet praticable.
 * ________________________________________________________________ */
public class Echelle extends ObjetInteractif {
    /* ________________________________________________________________
     * ACTION : Initialise l'échelle avec sa position et ses dimensions.
     * ________________________________________________________________ */
    public Echelle(Map map, int positionX, int positionY, int largeur, int hauteur) {
        super(map, positionX, positionY);
        hitbox = new int[]{largeur, hauteur};
        cheminImage = "images/echelle.png";
    }

    /* ________________________________________________________________
     * ACTION : Répète le sprite de l'échelle sur chaque tuile.
     * ________________________________________________________________ */
    @Override
    protected boolean estImageRepeteParTuile() {
        return true;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la taille de tuile du sprite répété.
     * ________________________________________________________________ */
    @Override
    protected int getTailleTuileDessin() {
        return map.getTailleTuile();
    }
}
