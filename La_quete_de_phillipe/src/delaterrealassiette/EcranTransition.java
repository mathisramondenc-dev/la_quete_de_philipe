package delaterrealassiette;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Écran affiché entre deux niveaux : « NIVEAU N TERMINÉ ! », le numéro du
 * niveau suivant et une barre de chargement.
 *
 * C'est un simple compte à rebours en ticks : demarrer() le lance, avancer()
 * le décrémente à chaque tick, dessiner() calcule l'opacité (fondu d'entrée
 * et de sortie) et la barre à partir du temps écoulé. Tant qu'il est actif,
 * la fenêtre met le jeu en pause. Après TICS_AVANT_PASSER ticks, un appui
 * sur Espace raccourcit l'écran en passant directement au fondu de sortie.
 */
public class EcranTransition {

    /** Durée totale : 3 s à 25 ticks par seconde. */
    public static final int DUREE_TICS = 75;
    /** Délai avant de pouvoir passer l'écran : 1 s. */
    public static final int TICS_AVANT_PASSER = 25;
    /** Durée du fondu d'apparition et de disparition. */
    private static final int TICS_FONDU = 8;

    private static final Color COULEUR_FOND = new Color(18, 20, 28);
    private static final Color COULEUR_OR = new Color(255, 220, 95);

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

        contexte.setColor(COULEUR_FOND);
        contexte.fillRect(0, 0, largeur, hauteur);

        int centreY = hauteur / 2;
        contexte.setColor(COULEUR_OR);
        dessinerTexteCentre(contexte, "NIVEAU " + niveauTermine + " TERMINÉ !",
                new Font("SansSerif", Font.BOLD, 56), largeur, centreY - 140);

        contexte.setColor(Color.WHITE);
        dessinerTexteCentre(contexte, "NIVEAU " + niveauSuivant,
                new Font("SansSerif", Font.BOLD, 140), largeur, centreY + 40);

        // Barre de chargement : pleine juste avant le fondu de sortie
        int largeurBarre = 600;
        int hauteurBarre = 16;
        int xBarre = (largeur - largeurBarre) / 2;
        int yBarre = centreY + 110;
        float progression = Math.min(1f, ticsEcoules / (float) (DUREE_TICS - TICS_FONDU));
        contexte.setColor(new Color(60, 60, 70));
        contexte.fillRect(xBarre, yBarre, largeurBarre, hauteurBarre);
        contexte.setColor(COULEUR_OR);
        contexte.fillRect(xBarre, yBarre, (int) (largeurBarre * progression), hauteurBarre);

        if (ticsEcoules >= TICS_AVANT_PASSER) {
            contexte.setColor(new Color(200, 200, 210));
            dessinerTexteCentre(contexte, "Appuie sur Espace pour continuer",
                    new Font("SansSerif", Font.PLAIN, 28), largeur, yBarre + 80);
        }

        contexte.setComposite(compositePrecedent);
    }

    private void dessinerTexteCentre(Graphics2D contexte, String texte, Font police,
            int largeur, int y) {
        contexte.setFont(police);
        int largeurTexte = contexte.getFontMetrics().stringWidth(texte);
        contexte.drawString(texte, (largeur - largeurTexte) / 2, y);
    }
}
