// FenetreDeJeu.java
package delaterrealassiette;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.Timer;

/* ________________________________________________________________
 * ACTION : Fenêtre de la partie. Lance la boucle de jeu (un tic toutes
 *          les DUREE_TIC ms), lit le clavier et la souris et dessine.
 * [INFO] Même principe que le FenetreDeJeu du prof : un framebuffer
 *        affiché dans un JLabel et un Timer qui appelle actionPerformed.
 * [INFO] Sans Jeu (mode debug), l'écran affiche l'état des 9 boutons.
 * [DEPENDANCE] ClavierSouris fournit le tableau de booléens.
 *              Jeu reçoit ce tableau dans miseAJour et dessine dans rendu.
 * ________________________________________________________________ */
public class FenetreDeJeu extends Fenetre implements ActionListener {

    public static final int DUREE_TIC = 40; // ms, soit 25 tics par seconde

    private static final String[] NOMS_BOUTONS = {
        "Haut (Z / flèche haut)",
        "Bas (S / flèche bas)",
        "Gauche (Q / flèche gauche)",
        "Droite (D / flèche droite)",
        "Philippe (1)",
        "Cyril (2)",
        "Norbert (3)",
        "Compétence (E)",
        "Sauter (Espace)"
    };

    private final Jeu jeu; // null en mode debug
    private final InfosPartie infosPartie; // null hors multijoueur
    private final JLabel ecran;
    private final BufferedImage framebuffer;
    private final Graphics2D contexte;
    private final ClavierSouris clavierSouris;
    private final Timer timer;

    private boolean[] etatBoutons = new boolean[ClavierSouris.NB_BOUTONS];
    private long numeroTic = 0;

    /* ________________________________________________________________
     * ACTION : Construit la fenêtre, le framebuffer et branche les écouteurs.
     * [INFO] La boucle ne démarre qu'avec demarrer().
     * ________________________________________________________________ */
    public FenetreDeJeu(Jeu jeu) {
        this(jeu, null);
    }

    /* ________________________________________________________________
     * ACTION : Construit la fenêtre pour une partie multijoueur.
     * [INFO] Le pseudo et l'identifiant de la partie sont affichés
     *        en haut à gauche de l'écran.
     * [DEPENDANCE] Appelé par FenetreHeberger et FenetreRejoindre.
     * ________________________________________________________________ */
    public FenetreDeJeu(Jeu jeu, InfosPartie infosPartie) {
        super();
        // Sans Jeu, la fenêtre reste sur l'écran de debug
        this.jeu = jeu;
        this.infosPartie = infosPartie;
        if (infosPartie != null) {
            setTitle(TITRE + " - " + infosPartie.getPseudo()
                    + " - partie " + infosPartie.getIdentifiantPartie());
        }

        ecran = new JLabel();
        ecran.setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        setContentPane(ecran);

        framebuffer = new BufferedImage(LARGEUR, HAUTEUR, BufferedImage.TYPE_INT_ARGB);
        ecran.setIcon(new ImageIcon(framebuffer));
        contexte = framebuffer.createGraphics();
        contexte.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        clavierSouris = new ClavierSouris();
        addKeyListener(clavierSouris);            // clavier : sur la fenêtre
        ecran.addMouseListener(clavierSouris);    // souris : sur l'écran, pour avoir
        ecran.addMouseMotionListener(clavierSouris); // des coordonnées dans l'image

        timer = new Timer(DUREE_TIC, this);
        finaliserFenetre();
    }

    /* ________________________________________________________________
     * ACTION : Démarre la boucle de jeu et donne le focus clavier.
     * [INFO] Sans le focus, les touches ne sont pas reçues.
     * ________________________________________________________________ */
    public void demarrer() {
        requestFocus();
        timer.start();
    }

    /* ________________________________________________________________
     * ACTION : Arrête la boucle de jeu.
     * ________________________________________________________________ */
    public void arreter() {
        timer.stop();
    }

    /* ________________________________________________________________
     * ACTION : Un tic de jeu : lecture des entrées, mise à jour, rendu.
     * [DEPENDANCE] Appelée par le Timer toutes les DUREE_TIC ms.
     * ________________________________________________________________ */
    @Override
    public void actionPerformed(ActionEvent evenement) {
        numeroTic++;
        boolean clic = clavierSouris.isClicSouris(); // avant getEtatBoutons()
        etatBoutons = clavierSouris.getEtatBoutons();

        if (jeu != null) {
            jeu.miseAJour(etatBoutons);
            jeu.rendu(contexte);
        } else {
            renduDebug(clic);
        }
        if (infosPartie != null) {
            renduInfosPartie();
        }

        ecran.repaint();
    }

    /* ________________________________________________________________
     * ACTION : Affiche le pseudo et l'identifiant de la partie en haut
     *          à gauche, par-dessus le jeu.
     * ________________________________________________________________ */
    private void renduInfosPartie() {
        String ligne1 = infosPartie.getPseudo() + (infosPartie.estHote() ? " (hôte)" : "");
        String ligne2 = "Partie " + infosPartie.getIdentifiantPartie();

        contexte.setColor(new Color(0, 0, 0, 150)); // noir transparent
        contexte.fillRoundRect(6, 6, 190, 44, 10, 10);
        contexte.setColor(Color.WHITE);
        contexte.setFont(new Font("SansSerif", Font.BOLD, 14));
        contexte.drawString(ligne1, 14, 24);
        contexte.setFont(new Font("SansSerif", Font.PLAIN, 12));
        contexte.drawString(ligne2, 14, 42);
    }

    /* ________________________________________________________________
     * ACTION : Retourne l'état des boutons lu au dernier tic.
     * ________________________________________________________________ */
    public boolean[] getEtatBoutons() {
        return etatBoutons.clone();
    }

    /* ________________________________________________________________
     * ACTION : Affiche l'état du clavier et de la souris (mode debug).
     * ________________________________________________________________ */
    private void renduDebug(boolean clic) {
        contexte.setColor(new Color(120, 170, 220));
        contexte.fillRect(0, 0, LARGEUR, HAUTEUR);

        contexte.setFont(new Font("Monospaced", Font.BOLD, 16));
        contexte.setColor(Color.BLACK);
        contexte.drawString("Tic n°" + numeroTic, 20, 30);
        contexte.drawString("Souris : (" + clavierSouris.getSourisX() + ", "
                + clavierSouris.getSourisY() + ")" + (clic ? "  CLIC" : ""), 300, 30);

        contexte.setFont(new Font("Monospaced", Font.PLAIN, 16));
        for (int i = 0; i < ClavierSouris.NB_BOUTONS; i++) {
            int y = 70 + i * 26;
            contexte.setColor(etatBoutons[i] ? new Color(0, 130, 0) : new Color(60, 60, 60));
            contexte.fillOval(20, y - 13, 14, 14);
            contexte.setColor(Color.BLACK);
            contexte.drawString("[" + i + "] " + NOMS_BOUTONS[i] + " = " + etatBoutons[i], 45, y);
        }
    }
}
