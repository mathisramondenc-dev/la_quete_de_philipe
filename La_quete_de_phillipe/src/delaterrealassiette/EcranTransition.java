package delaterrealassiette;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;

/* ________________________________________________________________
 * ACTION : Écran affiché entre deux niveaux ("NIVEAU 1 TERMINÉ !",
 *          puis le numéro du niveau suivant et une barre de chargement).
 * [INFO] Dure DUREE_TICS tics (3 secondes). Le joueur peut le passer
 *        avec Espace après TICS_AVANT_PASSER tics (1 seconde).
 * [INFO] Pendant la transition, le jeu est en pause.
 * [INFO] Image NOM_IMAGE à gauche (entière) sur un fond flouté de la même
 *        image ; texte et barre à droite. Image cherchée à côté des .java,
 *        puis dans images/. Sans image : fond uni comme avant.
 * [DEPENDANCE] FenetreDeJeu le démarre quand le numéro de niveau change,
 *              l'avance et le dessine à chaque tic.
 * ________________________________________________________________ */
public class EcranTransition {

    /** Durée totale : 3 s à 25 ticks par seconde. */
    public static final int DUREE_TICS = 75;
    /** Délai avant de pouvoir passer l'écran : 1 s. */
    public static final int TICS_AVANT_PASSER = 25;
    /** Durée du fondu d'apparition et de disparition. */
    private static final int TICS_FONDU = 8;

    private static final Color COULEUR_FOND = new Color(18, 20, 28);
    private static final Color COULEUR_OR = new Color(255, 220, 95);

    public static final String NOM_IMAGE = "transition.png";
    private static BufferedImage image;       // l'image entière
    private static BufferedImage imageFloue;  // petite version pour le fond flou
    private static boolean imageDejaCherchee = false;

    private int ticsRestants = 0;
    private int niveauTermine;
    private int niveauSuivant;
    private boolean espacePrecedemmentAppuye;

    public void demarrer(int niveauTermine, int niveauSuivant) {
        this.niveauTermine = niveauTermine;
        this.niveauSuivant = niveauSuivant;
        ticsRestants = DUREE_TICS;
        // Un Espace déjà enfoncé (saut) ne doit pas passer l'écran
        espacePrecedemmentAppuye = true;
    }

    public boolean estActive() {
        return ticsRestants > 0;
    }

    /**
     * Avance d'un tick ; un nouvel appui sur Espace (front montant) après le
     * délai minimal saute directement au fondu de sortie.
     */
    public void avancer(boolean espaceAppuye) {
        if (!estActive()) {
            return;
        }
        boolean nouvelAppui = espaceAppuye && !espacePrecedemmentAppuye;
        espacePrecedemmentAppuye = espaceAppuye;

        int ticsEcoules = DUREE_TICS - ticsRestants;
        if (nouvelAppui && ticsEcoules >= TICS_AVANT_PASSER && ticsRestants > TICS_FONDU) {
            ticsRestants = TICS_FONDU;
        }
        ticsRestants--;
    }

    /** Dessine l'écran par-dessus l'image du jeu, avec l'opacité du fondu en cours. */
    public void dessiner(Graphics2D contexte, int largeur, int hauteur) {
        int ticsEcoules = DUREE_TICS - ticsRestants;
        float opacite = Math.min(1f, Math.min(ticsEcoules, ticsRestants) / (float) TICS_FONDU);
        Composite compositePrecedent = contexte.getComposite();
        contexte.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                Math.max(0f, opacite)));

        // Centre horizontal du texte : milieu de l'écran, ou partie droite
        // si l'image est affichée à gauche
        int centreTexteX = largeur / 2;
        if (chargerImage()) {
            centreTexteX = dessinerFondImage(contexte, largeur, hauteur);
        } else {
            contexte.setColor(COULEUR_FOND);
            contexte.fillRect(0, 0, largeur, hauteur);
        }

        int centreY = hauteur / 2;
        contexte.setColor(COULEUR_OR);
        dessinerTexteCentre(contexte, "NIVEAU " + niveauTermine + " TERMINÉ !",
                new Font("SansSerif", Font.BOLD, 56), centreTexteX, centreY - 140);

        contexte.setColor(Color.WHITE);
        dessinerTexteCentre(contexte, "NIVEAU " + niveauSuivant,
                new Font("SansSerif", Font.BOLD, 140), centreTexteX, centreY + 40);

        // Barre de chargement : pleine juste avant le fondu de sortie
        int largeurBarre = 600;
        int hauteurBarre = 16;
        int xBarre = centreTexteX - largeurBarre / 2;
        int yBarre = centreY + 110;
        float progression = Math.min(1f, ticsEcoules / (float) (DUREE_TICS - TICS_FONDU));
        contexte.setColor(new Color(60, 60, 70));
        contexte.fillRect(xBarre, yBarre, largeurBarre, hauteurBarre);
        contexte.setColor(COULEUR_OR);
        contexte.fillRect(xBarre, yBarre, (int) (largeurBarre * progression), hauteurBarre);

        if (ticsEcoules >= TICS_AVANT_PASSER) {
            contexte.setColor(new Color(200, 200, 210));
            dessinerTexteCentre(contexte, "Appuie sur Espace pour continuer",
                    new Font("SansSerif", Font.PLAIN, 28), centreTexteX, yBarre + 80);
        }

        contexte.setComposite(compositePrecedent);
    }

    /* ________________________________________________________________
     * ACTION : Écrit un texte centré sur l'abscisse centreX, à la hauteur y.
     * ________________________________________________________________ */
    private void dessinerTexteCentre(Graphics2D contexte, String texte, Font police,
            int centreX, int y) {
        contexte.setFont(police);
        int largeurTexte = contexte.getFontMetrics().stringWidth(texte);
        contexte.drawString(texte, centreX - largeurTexte / 2, y);
    }

    /* ________________________________________________________________
     * ACTION : Dessine le fond flouté et assombri, puis l'image entière
     *          à gauche (toute la hauteur de l'écran).
     * [INFO] Retourne le centre horizontal de la zone libre à droite.
     * ________________________________________________________________ */
    private int dessinerFondImage(Graphics2D contexte, int largeur, int hauteur) {
        // Fond : petite image agrandie en lissant = effet flou, couvre l'écran
        double echelle = Math.max((double) largeur / imageFloue.getWidth(),
                (double) hauteur / imageFloue.getHeight());
        int l = (int) Math.ceil(imageFloue.getWidth() * echelle);
        int h = (int) Math.ceil(imageFloue.getHeight() * echelle);
        contexte.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        contexte.drawImage(imageFloue, (largeur - l) / 2, (hauteur - h) / 2, l, h, null);
        contexte.setColor(new Color(0, 0, 0, 170));
        contexte.fillRect(0, 0, largeur, hauteur);

        // Image entière, à gauche, sur toute la hauteur
        int hauteurImage = hauteur;
        int largeurImage = image.getWidth() * hauteurImage / image.getHeight();
        int xImage = largeur / 20;
        contexte.drawImage(image, xImage, 0, largeurImage, hauteurImage, null);

        // Ombre douce entre l'image et le texte
        contexte.setPaint(new java.awt.GradientPaint(xImage + largeurImage - 40, 0,
                new Color(0, 0, 0, 0), xImage + largeurImage + 60, 0, new Color(0, 0, 0, 120)));
        contexte.fillRect(xImage + largeurImage - 40, 0, 100, hauteur);

        int debutZoneTexte = xImage + largeurImage;
        return debutZoneTexte + (largeur - debutZoneTexte) / 2;
    }

    /* ________________________________________________________________
     * ACTION : Charge l'image une seule fois (false si introuvable).
     * [INFO] 1) à côté des .java  2) sinon images/transition.png
     * ________________________________________________________________ */
    private static boolean chargerImage() {
        if (!imageDejaCherchee) {
            imageDejaCherchee = true;
            File fichier = new File("images", NOM_IMAGE);
            try {
                URL ressource = EcranTransition.class.getResource(NOM_IMAGE);
                if (ressource != null) {
                    image = ImageIO.read(ressource);
                } else if (fichier.exists()) {
                    image = ImageIO.read(fichier);
                }
            } catch (Exception erreur) {
                image = null;
            }
            if (image == null) {
                System.err.println("Image de transition introuvable : mettre " + NOM_IMAGE
                        + " dans src/delaterrealassiette/ ou dans " + fichier.getAbsolutePath());
            } else {
                // Flou : réduire beaucoup l'image puis la ré-agrandir en
                // plusieurs étapes lissées (pas de gros carrés)
                BufferedImage flou = image;
                int largeurFlou = image.getWidth();
                int hauteurFlou = image.getHeight();
                for (int etape = 0; etape < 4; etape++) {      // réduction /16
                    largeurFlou /= 2;
                    hauteurFlou /= 2;
                    flou = redimensionner(flou, largeurFlou, hauteurFlou);
                }
                for (int etape = 0; etape < 3; etape++) {      // agrandissement x8
                    largeurFlou *= 2;
                    hauteurFlou *= 2;
                    flou = redimensionner(flou, largeurFlou, hauteurFlou);
                }
                imageFloue = flou;
            }
        }
        return image != null;
    }

    /* ________________________________________________________________
     * ACTION : Retourne une copie lissée de l'image à la taille demandée.
     * ________________________________________________________________ */
    private static BufferedImage redimensionner(BufferedImage source, int largeur, int hauteur) {
        BufferedImage resultat = new BufferedImage(Math.max(1, largeur), Math.max(1, hauteur),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resultat.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, resultat.getWidth(), resultat.getHeight(), null);
        g.dispose();
        return resultat;
    }
}
