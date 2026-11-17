// EcranTransition.java
package delaterrealassiette;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;

/* ________________________________________________________________
 * ACTION : Écran affiché entre deux niveaux ("NIVEAU 1 TERMINÉ !",
 *          puis le numéro du niveau suivant et une barre de chargement).
 * [INFO] Dure DUREE_TICS tics (3 secondes). Le joueur peut le passer
 *        avec Espace après TICS_AVANT_PASSER tics (1 seconde).
 * [INFO] Pendant la transition, le jeu est en pause.
 * [DEPENDANCE] FenetreDeJeu le démarre quand le numéro de niveau change,
 *              l'avance et le dessine à chaque tic.
 * ________________________________________________________________ */
public class EcranTransition {

    public static final int DUREE_TICS = 75;        // 3 s à 25 tics par seconde
    public static final int TICS_AVANT_PASSER = 25; // 1 s avant de pouvoir passer
    private static final int TICS_FONDU = 8;        // apparition / disparition

    private static final Color COULEUR_FOND = new Color(18, 20, 28);
    private static final Color COULEUR_OR = new Color(255, 220, 95);

    private int ticsRestants = 0;
    private int niveauTermine;
    private int niveauSuivant;
    private boolean espacePrecedemmentAppuye;

    /* ________________________________________________________________
     * ACTION : Lance la transition entre deux niveaux.
     * ________________________________________________________________ */
    public void demarrer(int niveauTermine, int niveauSuivant) {
        this.niveauTermine = niveauTermine;
        this.niveauSuivant = niveauSuivant;
        ticsRestants = DUREE_TICS;
        espacePrecedemmentAppuye = true; // ignore un Espace déjà enfoncé
    }

    /* ________________________________________________________________
     * ACTION : Indique si la transition est en cours.
     * ________________________________________________________________ */
    public boolean estActive() {
        return ticsRestants > 0;
    }

    /* ________________________________________________________________
     * ACTION : Avance la transition d'un tic.
     * [INFO] Un nouvel appui sur Espace (après 1 s) passe directement
     *        à la fin du fondu de sortie.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Dessine l'écran de transition par-dessus l'image du jeu.
     * ________________________________________________________________ */
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

        // Barre de chargement
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

    /* ________________________________________________________________
     * ACTION : Écrit un texte centré horizontalement à la hauteur y.
     * ________________________________________________________________ */
    private void dessinerTexteCentre(Graphics2D contexte, String texte, Font police,
            int largeur, int y) {
        contexte.setFont(police);
        int largeurTexte = contexte.getFontMetrics().stringWidth(texte);
        contexte.drawString(texte, (largeur - largeurTexte) / 2, y);
    }
}
