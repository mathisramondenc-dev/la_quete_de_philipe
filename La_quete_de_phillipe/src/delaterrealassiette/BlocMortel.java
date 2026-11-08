package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;

/* ________________________________________________________________
 * ACTION : Représente une zone traversable qui tue le joueur au contact.
 * ________________________________________________________________ */
public class BlocMortel extends Entite {
    private static final String cheminImageMortelle = "images/bloc_mortel.png";

    /* ________________________________________________________________
     * ACTION : Initialise la position et les dimensions en pixels.
     * ________________________________________________________________ */
    public BlocMortel(Map map, int positionX, int positionY, int largeur, int hauteur) {
        super(map, positionX, positionY);
        this.hitbox = new int[]{largeur, hauteur};
    }

    /* ________________________________________________________________
     * ACTION : Indique si le bloc chevauche une entité.
     * ________________________________________________________________ */
    public boolean enCollisionAvec(Entite entite) {
        int[] hitboxEntite = entite.getHitbox();
        return chevaucheRectangle(entite.getPositionX(), entite.getPositionY(),
                hitboxEntite[0], hitboxEntite[1]);
    }

    /* ________________________________________________________________
     * ACTION : Teste le chevauchement de deux rectangles.
     * ________________________________________________________________ */
    private boolean chevaucheRectangle(int x, int y, int largeur, int hauteur) {
        return x < positionX + hitbox[0] && x + largeur > positionX
                && y < positionY + hitbox[1] && y + hauteur > positionY;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hitbox du bloc mortel.
     * ________________________________________________________________ */
    @Override public int[] getHitbox() { return hitbox; }
    /* ________________________________________________________________
     * ACTION : Retourne une vitesse nulle.
     * ________________________________________________________________ */
    @Override public int getVitesse() { return 0; }
    /* ________________________________________________________________
     * ACTION : Retourne une impulsion nulle.
     * ________________________________________________________________ */
    @Override public int getForceSaut() { return 0; }
    /* ________________________________________________________________
     * ACTION : Retourne le chemin du sprite du bloc mortel.
     * ________________________________________________________________ */
    @Override public String getCheminImage() { return cheminImageMortelle; }
    /* ________________________________________________________________
     * ACTION : Ne réalise aucun déplacement.
     * ________________________________________________________________ */
    @Override public void mettreAJour() { }

    /* ________________________________________________________________
     * ACTION : Indique que le sprite remplit la zone par répétition.
     * ________________________________________________________________ */
    @Override
    protected boolean estImageRepeteParTuile() {
        return true;
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
