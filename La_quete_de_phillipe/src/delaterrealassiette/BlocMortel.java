package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;

/* ________________________________________________________________
 * ACTION : Représente une zone traversable qui tue le joueur au contact.
 * ________________________________________________________________ */
public class BlocMortel extends ObjetInteractif {
    private static final String cheminImageMortelle = "images/bloc_mortel.png";

    /* ________________________________________________________________
     * ACTION : Initialise la position et les dimensions en pixels.
     * ________________________________________________________________ */
    public BlocMortel(Map map, int positionX, int positionY, int largeur, int hauteur) {
        super(map, positionX, positionY);
        this.hitbox = new int[]{largeur, hauteur};
        this.cheminImage = cheminImageMortelle;
    }

    /* ________________________________________________________________
     * ACTION : Indique que le sprite remplit la zone par répétition.
     * ________________________________________________________________ */
    @Override
    protected boolean estImageRepeteParTuile() {
        return true;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la taille des tuiles utilisées pour le dessin.
     * ________________________________________________________________ */
    @Override
    protected int getTailleTuileDessin() {
        return map.getTailleTuile();
    }

    /* ________________________________________________________________
     * ACTION : Remplit la zone en rouge si le sprite est absent.
     * ________________________________________________________________ */
    @Override
    protected void dessinerImageManquante(Graphics2D contexte, int positionEcranX,
            int positionEcranY, int largeurZone, int hauteurZone) {
        Color couleurPrecedente = contexte.getColor();
        contexte.setColor(new Color(220, 35, 35));
        contexte.fillRect(positionEcranX, positionEcranY, largeurZone, hauteurZone);
        contexte.setColor(couleurPrecedente);
    }
}
