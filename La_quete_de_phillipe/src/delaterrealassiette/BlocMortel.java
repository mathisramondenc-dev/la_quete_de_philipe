package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/* ________________________________________________________________
 * ACTION : Représente une zone traversable qui tue le joueur au contact.
 * ________________________________________________________________ */
public class BlocMortel extends Entite {
    private static final String cheminImageMortelle = "images/bloc_mortel.png";
    private static final BufferedImage imageMortelle = chargerImageMortelle();
    private final int[] hitbox;

    /* ________________________________________________________________
     * ACTION : Charge le sprite du bloc mortel si le fichier est présent.
     * ________________________________________________________________ */
    private static BufferedImage chargerImageMortelle() {
        File fichier = new File(cheminImageMortelle);
        File repertoire = new File(System.getProperty("user.dir"));
        while (!fichier.isFile() && repertoire != null) {
            fichier = new File(repertoire, cheminImageMortelle);
            repertoire = repertoire.getParentFile();
        }
        try {
            return fichier.isFile() ? ImageIO.read(fichier) : null;
        } catch (IOException exception) {
            return null;
        }
    }

    /* ________________________________________________________________
     * ACTION : Initialise la position et les dimensions en pixels.
     * ________________________________________________________________ */
    public BlocMortel(Map map, int positionX, int positionY, int largeur, int hauteur) {
        super(map, positionX, positionY);
        hitbox = new int[]{largeur, hauteur};
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
     * ACTION : Dessine le sprite ou un rectangle rouge de remplacement.
     * ________________________________________________________________ */
    @Override public void dessiner(Graphics2D contexte, int x, int y) {
        if (imageMortelle != null) {
            contexte.drawImage(imageMortelle, x, y, hitbox[0], hitbox[1], null);
            return;
        }
        Color couleurPrecedente = contexte.getColor();
        contexte.setColor(new Color(220, 35, 35));
        contexte.fillRect(x, y, hitbox[0], hitbox[1]);
        contexte.setColor(couleurPrecedente);
    }
}
