package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Zone piège (lave, pics...) qui tue le joueur dès qu'il la touche.
 *
 * Elle ne bloque pas les déplacements : le joueur entre dedans et c'est la
 * classe Jeu qui détecte le contact et déclenche la mort. Le sprite est
 * répété tuile par tuile pour couvrir des zones de toutes tailles sans
 * déformation.
 */
public class BlocMortel extends ObjetInteractif {
    private static final String cheminImageMortelle = "images/bloc_mortel.png";

    /** Position et dimensions en pixels (converties depuis les tuiles par Map). */
    public BlocMortel(Map map, int positionX, int positionY, int largeur, int hauteur) {
        super(map, positionX, positionY);
        this.hitbox = new int[]{largeur, hauteur};
        this.cheminImage = cheminImageMortelle;
    }

    @Override
    protected boolean estImageRepeteParTuile() {
        return true;
    }

    @Override
    protected int getTailleTuileDessin() {
        return map.getTailleTuile();
    }

    /** Sans image : zone pleine rouge. */
    @Override
    protected void dessinerImageManquante(Graphics2D contexte, int positionEcranX,
            int positionEcranY, int largeurZone, int hauteurZone) {
        Color couleurPrecedente = contexte.getColor();
        contexte.setColor(new Color(220, 35, 35));
        contexte.fillRect(positionEcranX, positionEcranY, largeurZone, hauteurZone);
        contexte.setColor(couleurPrecedente);
    }
}
