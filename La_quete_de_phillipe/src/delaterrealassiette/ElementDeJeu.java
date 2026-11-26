package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;

/**
 * Racine de tout ce qui est posé dans le monde du jeu (joueurs, monstres,
 * caisses, ingrédients, leviers, portes...).
 *
 * Un élément = une position, une hitbox {largeur, hauteur}, un sprite et un
 * état actif. Repère : origine en bas à gauche, axe Y vers le haut (comme la
 * carte), la conversion vers l'écran est faite par l'appelant de dessiner().
 *
 * Stratégie de rendu : les images sont chargées une seule fois dans un cache
 * statique partagé par tous les éléments ; la version miroir (personnage
 * tourné vers la gauche) est calculée à la demande puis mise en cache elle
 * aussi. Les sous-classes adaptent l'affichage en redéfinissant quelques
 * méthodes « crochets » (estVisible, estMiroir, estImageRepeteParTuile...)
 * plutôt que de réécrire dessiner().
 */
public abstract class ElementDeJeu {
    protected int positionX;
    protected int positionY;
    protected int[] hitbox = new int[]{0, 0};
    protected String cheminImage = "";
    protected boolean estActif = true;
    private boolean regardeVersLaGauche;
    private static final java.util.Map<String, BufferedImage> cacheImages = new HashMap<>();
    /** Préfixe impossible dans un vrai chemin : distingue l'image miroir dans le cache. */
    private static final String cleImageMiroir = String.valueOf((char) 0) + "miroir:";

    protected ElementDeJeu(int positionX, int positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    /** Raccourci de chargerImageDepuisCache(chemin, false). */
    protected static synchronized BufferedImage chargerImageDepuisCache(String cheminImage) {
        return chargerImageDepuisCache(cheminImage, false);
    }

    /**
     * Renvoie l'image demandée en la chargeant au premier appel seulement.
     *
     * @param cheminImage chemin du fichier image
     * @param imageMiroir true pour obtenir l'image retournée horizontalement
     * @return l'image, ou null si le fichier est introuvable
     */
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

    /** Précharge le sprite (conservé pour les anciens appels des sous-classes). */
    protected void chargerImage(String cheminImage) {
        chargerImageDepuisCache(cheminImage);
    }

    /**
     * Lit l'image sur le disque. Si le chemin relatif n'existe pas depuis le
     * dossier courant, on remonte les dossiers parents : le jeu se lance ainsi
     * aussi bien depuis NetBeans que depuis un terminal.
     */
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

    /**
     * Dessine l'élément à l'écran. Sans image, un rectangle rouge de la taille
     * de la hitbox est tracé pour repérer le problème sans faire planter le jeu.
     *
     * @param positionEcranX abscisse écran du coin haut-gauche
     * @param positionEcranY ordonnée écran du coin haut-gauche
     */
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

    // ----- Crochets de rendu redéfinis par les sous-classes -----

    /** @return true si le sprite doit être retourné horizontalement */
    protected boolean estMiroir() {
        return regardeVersLaGauche;
    }

    /** @return false pour masquer temporairement l'élément (ex. clignotement) */
    protected boolean estVisible() {
        return true;
    }

    /** @return true pour répéter le sprite tuile par tuile au lieu de l'étirer */
    protected boolean estImageRepeteParTuile() {
        return false;
    }

    /** @return taille d'une tuile pour le rendu répété (0 si non utilisé) */
    protected int getTailleTuileDessin() {
        return 0;
    }

    /**
     * Pave la zone avec le sprite sans le déformer (blocs larges, lave...).
     * Un clip coupe les tuiles qui dépassent de la zone.
     */
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

    /** Rendu de secours : contour rouge de la hitbox. */
    protected void dessinerImageManquante(Graphics2D contexte, int positionEcranX,
            int positionEcranY, int largeur, int hauteur) {
        Color couleurPrecedente = contexte.getColor();
        contexte.setColor(Color.RED);
        contexte.drawRect(positionEcranX, positionEcranY, largeur - 1, hauteur - 1);
        contexte.setColor(couleurPrecedente);
    }

    // ----- Collisions entre éléments (rectangles alignés sur les axes) -----

    /** @return true si les hitbox des deux éléments se chevauchent */
    public boolean enCollisionAvec(ElementDeJeu autre) {
        int[] hitboxAutre = autre.getHitbox();
        return chevaucheRectangle(autre.getPositionX(), autre.getPositionY(),
                hitboxAutre[0], hitboxAutre[1]);
    }

    /** @return true si la hitbox chevauche le rectangle donné (repère du monde) */
    public boolean chevaucheRectangle(int x, int y, int largeur, int hauteur) {
        int[] dimensionsHitbox = getHitbox();
        return x < positionX + dimensionsHitbox[0] && x + largeur > positionX
                && y < positionY + dimensionsHitbox[1] && y + hauteur > positionY;
    }

    // ----- Accesseurs -----

    public void definirPosition(int positionX, int positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    public int getPositionX() { return positionX; }
    public int getPositionY() { return positionY; }
    public boolean isActif() { return estActif; }
    public void definirActif(boolean estActif) { this.estActif = estActif; }
    public void definirOrientationGauche(boolean regardeVersLaGauche) { this.regardeVersLaGauche = regardeVersLaGauche; }
    public boolean isRegardeVersLaGauche() { return regardeVersLaGauche; }
    /** @return {largeur, hauteur} en pixels */
    public int[] getHitbox() { return hitbox; }

    public String getCheminImage() { return cheminImage; }
}
