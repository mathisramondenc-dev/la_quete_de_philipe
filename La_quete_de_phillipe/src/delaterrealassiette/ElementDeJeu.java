package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
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
    private static final java.util.Map<String, BufferedImage> cacheImages = new HashMap<>();
    private static final String cleImageMiroir = String.valueOf((char) 0) + "miroir:";

    protected ElementDeJeu(int positionX, int positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    /* ________________________________________________________________
     * ACTION : Retourne une image normale ou miroir depuis le cache partagé.
     * ________________________________________________________________ */
    protected static synchronized BufferedImage chargerImageDepuisCache(String cheminImage) {
        return chargerImageDepuisCache(cheminImage, false);
    }

    /* ________________________________________________________________
     * ACTION : Charge une image et conserve séparément son éventuel miroir.
     * ________________________________________________________________ */
    protected static synchronized BufferedImage chargerImageDepuisCache(
            String cheminImage, boolean imageMiroir) {
        String cleImage = imageMiroir ? cleImageMiroir + cheminImage : cheminImage;
        if (!cacheImages.containsKey(cleImage)) {
            BufferedImage image = imageMiroir
                    ? chargerImageDepuisCache(cheminImage, false)
                    : chargerImageDepuisFichier(cheminImage);
            if (imageMiroir && image != null) {
                BufferedImage imageRetournee = new BufferedImage(
                        image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                AffineTransform transformation = AffineTransform.getScaleInstance(-1, 1);
                transformation.translate(-image.getWidth(), 0);
                Graphics2D contexte = imageRetournee.createGraphics();
                contexte.drawImage(image, transformation, null);
                contexte.dispose();
                image = imageRetournee;
            }
            cacheImages.put(cleImage, image);
        }
        return cacheImages.get(cleImage);
    }

    /* ________________________________________________________________
     * ACTION : Charge une image pour les appels historiques de l'entité.
     * ________________________________________________________________ */
    protected void chargerImage(String cheminImage) {
        chargerImageDepuisCache(cheminImage);
    }

    /* ________________________________________________________________
     * ACTION : Charge un fichier depuis son chemin relatif au projet.
     * ________________________________________________________________ */
    private static BufferedImage chargerImageDepuisFichier(String cheminImage) {
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
        try {
            return fichier.isFile() ? ImageIO.read(fichier) : null;
        } catch (IOException exception) {
            return null;
        }
    }

    /* ________________________________________________________________
     * ACTION : Dessine le sprite ou son rendu de remplacement.
     * ________________________________________________________________ */
    public void dessiner(Graphics2D contexte, int positionEcranX, int positionEcranY) {
        if (!estActif || !estVisible()) {
            return;
        }
        int[] dimensionsHitbox = getHitbox();
        BufferedImage image = chargerImageDepuisCache(getCheminImage(), estMiroir());
        if (image == null) {
            dessinerImageManquante(contexte, positionEcranX, positionEcranY,
                    dimensionsHitbox[0], dimensionsHitbox[1]);
        } else if (estImageRepeteParTuile()) {
            dessinerImageRepeteParTuile(contexte, image, positionEcranX, positionEcranY,
                    dimensionsHitbox[0], dimensionsHitbox[1], getTailleTuileDessin());
        } else {
            contexte.drawImage(image, positionEcranX, positionEcranY,
                    dimensionsHitbox[0], dimensionsHitbox[1], null);
        }
    }

    /* ________________________________________________________________
     * ACTION : Indique si le sprite doit être affiché en miroir horizontal.
     * ________________________________________________________________ */
    protected boolean estMiroir() {
        return regardeVersLaGauche;
    }

    /* ________________________________________________________________
     * ACTION : Indique si l'élément doit être affiché.
     * ________________________________________________________________ */
    protected boolean estVisible() {
        return true;
    }

    /* ________________________________________________________________
     * ACTION : Indique si le sprite est répété sur chaque tuile.
     * ________________________________________________________________ */
    protected boolean estImageRepeteParTuile() {
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la taille de tuile utilisée par le rendu répété.
     * ________________________________________________________________ */
    protected int getTailleTuileDessin() {
        return 0;
    }

    /* ________________________________________________________________
     * ACTION : Dessine le sprite sur chaque tuile sans étirement.
     * [INFO] Les bords sont limités aux dimensions de la zone.
     * ________________________________________________________________ */
    protected void dessinerImageRepeteParTuile(Graphics2D contexte, BufferedImage image,
            int positionEcranX, int positionEcranY, int largeurZone, int hauteurZone,
            int tailleTuile) {
        Graphics2D contexteTuiles = (Graphics2D) contexte.create();
        contexteTuiles.clipRect(positionEcranX, positionEcranY, largeurZone, hauteurZone);
        for (int ligne = 0; ligne < hauteurZone; ligne += tailleTuile) {
            for (int colonne = 0; colonne < largeurZone; colonne += tailleTuile) {
                contexteTuiles.drawImage(image, positionEcranX + colonne, positionEcranY + ligne,
                        tailleTuile, tailleTuile, null);
            }
        }
        contexteTuiles.dispose();
    }

    /* ________________________________________________________________
     * ACTION : Dessine le contour rouge lorsque le sprite est absent.
     * ________________________________________________________________ */
    protected void dessinerImageManquante(Graphics2D contexte, int positionEcranX,
            int positionEcranY, int largeur, int hauteur) {
        Color couleurPrecedente = contexte.getColor();
        contexte.setColor(Color.RED);
        contexte.drawRect(positionEcranX, positionEcranY, largeur - 1, hauteur - 1);
        contexte.setColor(couleurPrecedente);
    }

    /* ________________________________________________________________
     * ACTION : Compare les rectangles de collision des deux éléments.
     * ________________________________________________________________ */
    public boolean enCollisionAvec(ElementDeJeu autre) {
        int[] hitboxAutre = autre.getHitbox();
        int[] dimensionsHitbox = getHitbox();
        return positionX < autre.positionX + hitboxAutre[0]
                && positionX + dimensionsHitbox[0] > autre.positionX
                && positionY < autre.positionY + hitboxAutre[1]
                && positionY + dimensionsHitbox[1] > autre.positionY;
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
