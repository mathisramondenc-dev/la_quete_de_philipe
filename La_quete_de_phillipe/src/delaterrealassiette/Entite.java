package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/* ________________________________________________________________
 * ACTION : Centralise la position et les déplacements des entités.
 * [INFO] L'origine de la hitbox est le coin inférieur gauche.
 * [DEPENDANCE] La map est créée par Jeu et fournit collisionHitbox.
 * ________________________________________________________________ */
public abstract class Entite {
    protected int positionX;
    protected int positionY;
    protected int vitesseX;
    protected int vitesseY;
    protected int gravite = -6;
    protected int vitesseYMin = -24;
    protected final Map map;
    private BufferedImage image;
    private String cheminImageCharge;
    private boolean regardeVersLaGauche;

    /* ________________________________________________________________
     * ACTION : Initialise la position et la map de l'entité.
     * [INFO] La map n'est jamais recréée par l'entité.
     * [DEPENDANCE] Jeu transmet son unique instance de Map.
     * ________________________________________________________________ */
    public Entite(Map map, int positionX, int positionY) {
        this.map = map;
        this.positionX = positionX;
        this.positionY = positionY;
    }

    /* ________________________________________________________________
     * ACTION : Initialise l'entité à l'origine.
     * [DEPENDANCE] Jeu transmet son unique instance de Map.
     * ________________________________________________________________ */
    public Entite(Map map) {
        this(map, 0, 0);
    }

    /* ________________________________________________________________
     * ACTION : Charge le sprite de l'entité depuis son chemin relatif.
     * [INFO] Le chemin est résolu depuis le projet et l'image absente reste facultative.
     * ________________________________________________________________ */
    protected void chargerImage(String cheminImage) {
        image = null;
        cheminImageCharge = cheminImage;
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
            if (fichier.isFile()) {
                image = ImageIO.read(fichier);
            }
        } catch (IOException exception) {
            image = null;
        }
    }

    /* ________________________________________________________________
     * ACTION : Définit l'orientation du sprite.
     * ________________________________________________________________ */
    public void definirOrientationGauche(boolean regardeVersLaGauche) {
        this.regardeVersLaGauche = regardeVersLaGauche;
    }

    /* ________________________________________________________________
     * ACTION : Retourne l'orientation gauche de l'entité.
     * ________________________________________________________________ */
    public boolean isRegardeVersLaGauche() {
        return regardeVersLaGauche;
    }

    /* ________________________________________________________________
     * ACTION : Dessine le sprite ou la hitbox de remplacement.
     * [INFO] Les coordonnées sont celles du coin supérieur gauche à l'écran.
     * ________________________________________________________________ */
    public void dessiner(Graphics2D contexte, int positionEcranX, int positionEcranY) {
        String cheminImage = getCheminImage();
        if (image == null || !java.util.Objects.equals(cheminImageCharge, cheminImage)) {
            chargerImage(cheminImage);
        }

        int[] hitbox = getHitbox();
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
     * ACTION : Définit la position de l'entité.
     * ________________________________________________________________ */
    public void definirPosition(int positionX, int positionY) {
        this.positionX = positionX;
        this.positionY = positionY;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la map partagée.
     * ________________________________________________________________ */
    public Map getMap() {
        return map;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la position sur l'axe x.
     * ________________________________________________________________ */
    public int getPositionX() {
        return positionX;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la position sur l'axe y.
     * ________________________________________________________________ */
    public int getPositionY() {
        return positionY;
    }

    /* ________________________________________________________________
     * ACTION : Applique les déplacements sur les deux axes.
     * ________________________________________________________________ */
    public void deplacer() {
        deplacerX();
        deplacerY();
    }

    /* ________________________________________________________________
     * ACTION : Déplace l'entité sur l'axe x pixel par pixel.
     * [INFO] Une collision annule la vitesse sur l'axe x.
     * [DEPENDANCE] La map fournit collisionHitbox.
     * ________________________________________________________________ */
    protected void deplacerX() {
        int[] hitbox = getHitbox();
        int largeurHitbox = hitbox[0];
        int hauteurHitbox = hitbox[1];
        int direction = Integer.signum(vitesseX);
        int distance = Math.abs(vitesseX);

        for (int pixel = 0; pixel < distance; pixel++) {
            int prochainePositionX = positionX + direction;

            if (map.collisionHitbox(
                    prochainePositionX, positionY, largeurHitbox, hauteurHitbox)) {
                vitesseX = 0;
                break;
            }

            positionX = prochainePositionX;
        }
    }

    /* ________________________________________________________________
     * ACTION : Déplace l'entité sur l'axe y pixel par pixel.
     * [INFO] Une collision annule la vitesse sur l'axe y.
     * [DEPENDANCE] La map fournit collisionHitbox.
     * ________________________________________________________________ */
    protected void deplacerY() {
        int[] hitbox = getHitbox();
        int largeurHitbox = hitbox[0];
        int hauteurHitbox = hitbox[1];
        int direction = Integer.signum(vitesseY);
        int distance = Math.abs(vitesseY);

        for (int pixel = 0; pixel < distance; pixel++) {
            int prochainePositionY = positionY + direction;

            if (map.collisionHitbox(
                    positionX, prochainePositionY, largeurHitbox, hauteurHitbox)) {
                vitesseY = 0;
                break;
            }

            positionY = prochainePositionY;
        }
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hitbox sous la forme largeur puis hauteur.
     * ________________________________________________________________ */
    public abstract int[] getHitbox();

    /* ________________________________________________________________
     * ACTION : Retourne la vitesse de déplacement.
     * ________________________________________________________________ */
    public abstract int getVitesse();

    /* ________________________________________________________________
     * ACTION : Retourne l'impulsion du saut.
     * ________________________________________________________________ */
    public abstract int getForceSaut();

    /* ________________________________________________________________
     * ACTION : Retourne le chemin de l'image.
     * ________________________________________________________________ */
    public abstract String getCheminImage();

    /* ________________________________________________________________
     * ACTION : Actualise les intentions et les déplacements.
     * ________________________________________________________________ */
    public abstract void mettreAJour();
}