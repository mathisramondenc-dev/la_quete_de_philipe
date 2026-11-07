package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;

/* ________________________________________________________________
 * ACTION : Regroupe la position, la hitbox, le sprite et l'état actif.
 * [INFO] L'origine est en bas à gauche, avec l'axe vertical orienté vers le haut.
 * [DEPENDANCE] Entite et ObjetInteractif héritent de cet élément commun.
 * ________________________________________________________________ */
public abstract class ElementDeJeu {
    protected int positionX;
    protected int positionY;
    protected int[] hitbox = new int[]{0, 0};
    protected String cheminImage = "";
    protected boolean estActif = true;
    private boolean regardeVersLaGauche;
    private static final java.util.Map<String, BufferedImage> CACHE_IMAGES = new HashMap<>();

    protected ElementDeJeu(int positionX, int positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    /* ________________________________________________________________
     * ACTION : Retourne l'image en cache, en chargeant le fichier si nécessaire.
     * ________________________________________________________________ */
    protected static BufferedImage chargerImageDepuisCache(String cheminImage) {
        if (!CACHE_IMAGES.containsKey(cheminImage)) {
            File fichier = new File(cheminImage);
            if (cheminImage.startsWith("/sprites/")) {
                fichier = new File("images" + cheminImage);
            }
            if (!fichier.isAbsolute() && !fichier.isFile()) {
                File repertoire = new File(System.getProperty("user.dir"));
                while (repertoire != null && !fichier.isFile()) {
                    fichier = new File(repertoire, cheminImage.startsWith("/sprites/")
                            ? "images" + cheminImage : cheminImage);
                    repertoire = repertoire.getParentFile();
                }
            }
            BufferedImage image = null;
            try {
                if (fichier.isFile()) {
                    image = ImageIO.read(fichier);
                }
            } catch (IOException exception) {
                image = null;
            }
            CACHE_IMAGES.put(cheminImage, image);
        }
        return CACHE_IMAGES.get(cheminImage);
    }

    /* ________________________________________________________________
     * ACTION : Dessine le sprite ou une hitbox de remplacement.
     * ________________________________________________________________ */
    public void dessiner(Graphics2D contexte, int positionEcranX, int positionEcranY) {
        if (!estActif) {
            return;
        }
        int[] hitbox = getHitbox();
        BufferedImage image = chargerImageDepuisCache(getCheminImage());
        if (image != null) {
            if (regardeVersLaGauche) {
                contexte.drawImage(image, positionEcranX + hitbox[0], positionEcranY,
                        -hitbox[0], hitbox[1], null);
            } else {
                contexte.drawImage(image, positionEcranX, positionEcranY, hitbox[0], hitbox[1], null);
            }
        } else {
            Color couleurPrecedente = contexte.getColor();
            contexte.setColor(Color.RED);
            contexte.drawRect(positionEcranX, positionEcranY, hitbox[0] - 1, hitbox[1] - 1);
            contexte.setColor(couleurPrecedente);
        }
    }

    /* ________________________________________________________________
     * ACTION : Compare les rectangles de collision des deux éléments.
     * ________________________________________________________________ */
    public boolean enCollisionAvec(ElementDeJeu autre) {
        int[] hitboxAutre = autre.getHitbox();
        int[] hitbox = getHitbox();
        return positionX < autre.positionX + hitboxAutre[0]
                && positionX + hitbox[0] > autre.positionX
                && positionY < autre.positionY + hitboxAutre[1]
                && positionY + hitbox[1] > autre.positionY;
    }

    public void definirPosition(int positionX, int positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la position sur l’axe x.
     * ________________________________________________________________ */
    public int getPositionX() { return positionX; }
    /* ________________________________________________________________
     * ACTION : Retourne la position sur l’axe y.
     * ________________________________________________________________ */
    public int getPositionY() { return positionY; }
    /* ________________________________________________________________
     * ACTION : Indique si l’élément est actif.
     * ________________________________________________________________ */
    public boolean isActif() { return estActif; }
    /* ________________________________________________________________
     * ACTION : Modifie l’état actif de l’élément.
     * ________________________________________________________________ */
    public void definirActif(boolean estActif) { this.estActif = estActif; }
    /* ________________________________________________________________
     * ACTION : Définit l’orientation du sprite.
     * ________________________________________________________________ */
    public void definirOrientationGauche(boolean regardeVersLaGauche) { this.regardeVersLaGauche = regardeVersLaGauche; }
    /* ________________________________________________________________
     * ACTION : Retourne l’orientation du sprite.
     * ________________________________________________________________ */
    public boolean isRegardeVersLaGauche() { return regardeVersLaGauche; }
    /* ________________________________________________________________
     * ACTION : Retourne la hitbox largeur-hauteur.
     * ________________________________________________________________ */
    public abstract int[] getHitbox();
    /* ________________________________________________________________
     * ACTION : Retourne le chemin du sprite.
     * ________________________________________________________________ */
    public abstract String getCheminImage();
}
