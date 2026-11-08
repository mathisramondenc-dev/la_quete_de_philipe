package delaterrealassiette;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/* ________________________________________________________________
 * ACTION : Fenêtre de la partie. Lance la boucle de jeu (un tic toutes
 *          les DUREE_TIC ms), lit le clavier et la souris et dessine.
 * [INFO] L'image logique 1920x1056 est mise à l'échelle dans le panneau
 * [INFO] Sans Jeu (mode debug), l'écran affiche l'état des 9 boutons
 *        et tourne à IMAGES_PAR_SECONDE_DEBUG images par seconde.
 * [DEPENDANCE] ClavierSouris fournit le tableau de booléens.
 *              Jeu reçoit ce tableau dans miseAJour et dessine dans rendu.
 * ________________________________________________________________ */
public class FenetreDeJeu extends Fenetre implements ActionListener {

    public static final int DUREE_TIC = 40; // ms, soit 25 tics par seconde
    public static final int IMAGES_PAR_SECONDE_DEBUG = 24; // mode debug
    public static final int LARGEUR_JEU = 60 * 32; // 1920 px
    public static final int HAUTEUR_JEU = 33 * 32; // 1056 px

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
    private final JPanel ecran;
    private final BufferedImage framebuffer;
    private final Graphics2D contexte;
    private final ClavierSouris clavierSouris;
    private final Timer timer;
    private boolean pleinEcran;
    private Rectangle limitesFenetreAvantPleinEcran;
    private int etatFenetreAvantPleinEcran;

    private boolean[] etatBoutons = new boolean[ClavierSouris.NB_BOUTONS];
    private long numeroTic = 0;

    // Mesure des images par seconde (affichée en mode debug)
    private long debutBoucle;
    private long debutSeconde;
    private int imagesDansLaSeconde = 0;
    private int imagesParSecondeMesurees = 0;

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
     * [DEPENDANCE] Appelé par FenetreConnexion.
     * ________________________________________________________________ */
    public FenetreDeJeu(Jeu jeu, InfosPartie infosPartie) {
        super();
        this.jeu = jeu;
        this.infosPartie = infosPartie;
        if (infosPartie != null) {
            setTitle(TITRE + " - " + infosPartie.getPseudo()
                    + " - partie " + infosPartie.getIdentifiantPartie());
        }

        // Panneau d'affichage mis à l'échelle
        ecran = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphismes) {
                super.paintComponent(graphismes);
                Graphics2D graphismesDeuxD = (Graphics2D) graphismes.create();
                graphismesDeuxD.setColor(Color.BLACK);
                graphismesDeuxD.fillRect(0, 0, getWidth(), getHeight());
                graphismesDeuxD.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                Rectangle zoneAffichage = calculerZoneAffichage();
                graphismesDeuxD.drawImage(framebuffer, zoneAffichage.x, zoneAffichage.y,
                        zoneAffichage.width, zoneAffichage.height, null);
                graphismesDeuxD.dispose();
            }
        };
        ecran.setOpaque(true);
        ecran.setBackground(Color.BLACK);
        ecran.setPreferredSize(new Dimension(LARGEUR_JEU, HAUTEUR_JEU));
        setContentPane(ecran);

        framebuffer = new BufferedImage(LARGEUR_JEU, HAUTEUR_JEU, BufferedImage.TYPE_INT_ARGB);
        contexte = framebuffer.createGraphics();
        contexte.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Entrées clavier et souris
        clavierSouris = new ClavierSouris();
        addKeyListener(clavierSouris);
        ConvertisseurSouris convertisseurSouris = new ConvertisseurSouris();
        ecran.addMouseListener(convertisseurSouris);
        ecran.addMouseMotionListener(convertisseurSouris);

        // Raccourci plein écran
        ecran.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "basculerPleinEcran");
        ecran.getActionMap().put("basculerPleinEcran", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evenement) {
                basculerPleinEcran();
            }
        });

        timer = new Timer(DUREE_TIC, this);
        setResizable(true);
        pack();
        setLocationRelativeTo(null);
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    /* ________________________________________________________________
     * ACTION : Calcule la zone de rendu centrée en conservant le ratio logique.
     * ________________________________________________________________ */
    private Rectangle calculerZoneAffichage() {
        int largeurEcran = ecran.getWidth();
        int hauteurEcran = ecran.getHeight();
        double facteur = Math.min((double) largeurEcran / LARGEUR_JEU,
                (double) hauteurEcran / HAUTEUR_JEU);
        int largeurAffichage = (int) Math.round(LARGEUR_JEU * facteur);
        int hauteurAffichage = (int) Math.round(HAUTEUR_JEU * facteur);
        return new Rectangle((largeurEcran - largeurAffichage) / 2,
                (hauteurEcran - hauteurAffichage) / 2,
                largeurAffichage, hauteurAffichage);
    }

    /* ________________________________________________________________
     * ACTION : Bascule entre la fenêtre maximisée et le plein écran sans bordure.
     * ________________________________________________________________ */
    private void basculerPleinEcran() {
        GraphicsDevice appareilAffichage = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!pleinEcran) {
            limitesFenetreAvantPleinEcran = getBounds();
            etatFenetreAvantPleinEcran = getExtendedState();
            dispose();
            setUndecorated(true);
            appareilAffichage.setFullScreenWindow(this);
            setVisible(true);
            pleinEcran = true;
        } else {
            appareilAffichage.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setBounds(limitesFenetreAvantPleinEcran);
            setExtendedState(etatFenetreAvantPleinEcran);
            setVisible(true);
            pleinEcran = false;
        }
        ecran.requestFocusInWindow();
    }

    /* ________________________________________________________________
     * ACTION : Convertit les coordonnées souris vers l'espace logique puis
     *          transmet l'événement à ClavierSouris.
     * [DEPENDANCE] ClavierSouris (reste inchangé)
     * ________________________________________________________________ */
    private class ConvertisseurSouris extends MouseAdapter {

        private MouseEvent convertirEvenementSouris(MouseEvent evenement) {
            Rectangle zoneAffichage = calculerZoneAffichage();
            int positionX = (int) ((evenement.getX() - zoneAffichage.x)
                    * (double) LARGEUR_JEU / zoneAffichage.width);
            int positionY = (int) ((evenement.getY() - zoneAffichage.y)
                    * (double) HAUTEUR_JEU / zoneAffichage.height);
            return new MouseEvent(ecran, evenement.getID(), evenement.getWhen(),
                    evenement.getModifiersEx(), positionX, positionY,
                    evenement.getClickCount(), evenement.isPopupTrigger(),
                    evenement.getButton());
        }

        @Override
        public void mousePressed(MouseEvent evenement) {
            clavierSouris.mousePressed(convertirEvenementSouris(evenement));
        }

        @Override
        public void mouseReleased(MouseEvent evenement) {
            clavierSouris.mouseReleased(convertirEvenementSouris(evenement));
        }

        @Override
        public void mouseClicked(MouseEvent evenement) {
            clavierSouris.mouseClicked(convertirEvenementSouris(evenement));
        }

        @Override
        public void mouseEntered(MouseEvent evenement) {
            clavierSouris.mouseEntered(convertirEvenementSouris(evenement));
        }

        @Override
        public void mouseExited(MouseEvent evenement) {
            clavierSouris.mouseExited(convertirEvenementSouris(evenement));
        }

        @Override
        public void mouseMoved(MouseEvent evenement) {
            clavierSouris.mouseMoved(convertirEvenementSouris(evenement));
        }

        @Override
        public void mouseDragged(MouseEvent evenement) {
            clavierSouris.mouseDragged(convertirEvenementSouris(evenement));
        }
    }

    /* ________________________________________________________________
     * ACTION : Démarre la boucle de jeu et donne le focus clavier.
     * [INFO] Sans le focus, les touches ne sont pas reçues.
     * ________________________________________________________________ */
    public void demarrer() {
        requestFocus();
        debutBoucle = System.currentTimeMillis();
        debutSeconde = debutBoucle;
        if (jeu == null) {
            timer.setDelay(1000 / IMAGES_PAR_SECONDE_DEBUG);
        }
        timer.start();
    }

    /* ________________________________________________________________
     * ACTION : En mode debug, règle le délai avant le prochain tic pour
     *          avoir exactement IMAGES_PAR_SECONDE_DEBUG images par seconde.
     * ________________________________________________________________ */
    private void reglerProchainTicDebug() {
        long prochaineImage = debutBoucle + (numeroTic + 1) * 1000 / IMAGES_PAR_SECONDE_DEBUG;
        long attente = prochaineImage - System.currentTimeMillis();
        timer.setDelay((int) Math.max(1, attente));
    }

    /* ________________________________________________________________
     * ACTION : Compte les images affichées pendant chaque seconde.
     * ________________________________________________________________ */
    private void mesurerImagesParSeconde() {
        imagesDansLaSeconde++;
        long maintenant = System.currentTimeMillis();
        if (maintenant - debutSeconde >= 1000) {
            imagesParSecondeMesurees = imagesDansLaSeconde;
            imagesDansLaSeconde = 0;
            debutSeconde += 1000;
        }
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
        boolean clic = clavierSouris.isClicSouris();
        etatBoutons = clavierSouris.getEtatBoutons();

        if (jeu != null) {
            if (!jeu.isVictoire()) {
                jeu.miseAJour(etatBoutons);
            }
            if (jeu.isVictoire()) {
                timer.stop();
                dessinerVictoire();
            } else {
                jeu.rendu(contexte);
                jeu.dessinerIngredients(contexte);
                jeu.getInventaireRecette().dessiner(contexte, LARGEUR_JEU);
            }
        } else {
            renduDebug(clic);
            reglerProchainTicDebug();
        }
        mesurerImagesParSeconde();
        if (infosPartie != null) {
            renduInfosPartie();
        }

        ecran.repaint();
    }

    /* ________________________________________________________________
     * ACTION : Affiche l'écran de victoire final.
     * ________________________________________________________________ */
    private void dessinerVictoire() {
        contexte.setColor(new Color(18, 20, 28));
        contexte.fillRect(0, 0, LARGEUR_JEU, HAUTEUR_JEU);
        contexte.setColor(new Color(255, 220, 95));
        contexte.setFont(new Font("SansSerif", Font.BOLD, 104));
        String texte = "BRAVO !";
        int positionX = (LARGEUR_JEU - contexte.getFontMetrics().stringWidth(texte)) / 2;
        int positionY = (HAUTEUR_JEU - contexte.getFontMetrics().getHeight()) / 2
                + contexte.getFontMetrics().getAscent();
        contexte.drawString(texte, positionX, positionY);
    }

    /* ________________________________________________________________
     * ACTION : Affiche le pseudo et l'identifiant de la partie en haut
     *          à gauche, par-dessus le jeu.
     * ________________________________________________________________ */
    private void renduInfosPartie() {
        String lignePseudo = infosPartie.getPseudo();
        String lignePartie = "Partie " + infosPartie.getIdentifiantPartie();

        contexte.setColor(new Color(0, 0, 0, 150));
        contexte.fillRoundRect(6, 6, 190, 44, 10, 10);
        contexte.setColor(Color.WHITE);
        contexte.setFont(new Font("SansSerif", Font.BOLD, 14));
        contexte.drawString(lignePseudo, 14, 24);
        contexte.setFont(new Font("SansSerif", Font.PLAIN, 12));
        contexte.drawString(lignePartie, 14, 42);
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
        contexte.fillRect(0, 0, LARGEUR_JEU, HAUTEUR_JEU);

        contexte.setFont(new Font("Monospaced", Font.BOLD, 16));
        contexte.setColor(Color.BLACK);
        contexte.drawString("Tic n°" + numeroTic, 20, 30);
        contexte.drawString("Images/s : " + imagesParSecondeMesurees
                + " (objectif " + IMAGES_PAR_SECONDE_DEBUG + ")", 180, 30);
        contexte.drawString("Souris : (" + clavierSouris.getSourisX() + ", "
                + clavierSouris.getSourisY() + ")" + (clic ? "  CLIC" : ""), 520, 30);

        contexte.setFont(new Font("Monospaced", Font.PLAIN, 16));
        for (int indice = 0; indice < ClavierSouris.NB_BOUTONS; indice++) {
            int positionY = 70 + indice * 26;
            contexte.setColor(etatBoutons[indice] ? new Color(0, 130, 0) : new Color(60, 60, 60));
            contexte.fillOval(20, positionY - 13, 14, 14);
            contexte.setColor(Color.BLACK);
            contexte.drawString("[" + indice + "] " + NOMS_BOUTONS[indice]
                    + " = " + etatBoutons[indice], 45, positionY);
        }
    }
}