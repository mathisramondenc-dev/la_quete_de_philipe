package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Représente une caisse soumise à la gravité et aux collisions.
 * ________________________________________________________________ */
public class Caisse extends Entite {
    /* ________________________________________________________________
     * ACTION : Initialise une caisse d'une tuile à la position indiquée.
     * ________________________________________________________________ */
    public Caisse(Map map, int positionX, int positionY) {
        super(map, positionX, positionY);
        hitbox = new int[]{map.getTailleTuile(), map.getTailleTuile()};
        cheminImage = "images/caisse.png";
    }

    /* ________________________________________________________________
     * ACTION : Retourne la vitesse horizontale de la caisse.
     * ________________________________________________________________ */
    @Override
    public int getVitesse() {
        return 0;
    }

    /* ________________________________________________________________
     * ACTION : Indique que la caisse ne peut pas sauter.
     * ________________________________________________________________ */
    @Override
    public int getForceSaut() {
        return 0;
    }

    /* ________________________________________________________________
     * ACTION : Actualise la gravité de la caisse.
     * ________________________________________________________________ */
    @Override
    public void mettreAJour() {
        vitesseX = 0;
        if (estAuSol()) {
            vitesseY = 0;
        } else {
            vitesseY = Math.max(vitesseY + gravite, vitesseYMin);
        }
        deplacer();
    }

    /* ________________________________________________________________
     * ACTION : Détecte le support sous la caisse sans compter sa propre hitbox.
     * ________________________________________________________________ */
    @Override
    protected boolean estAuSol() {
        return (map.collisionHitboxSansCaisses(positionX, positionY - 1,
                hitbox[0], hitbox[1], this)
                || map.estSurSommetEchelle(positionX, positionY, hitbox[0]))
                && vitesseY <= 0;
    }
}
