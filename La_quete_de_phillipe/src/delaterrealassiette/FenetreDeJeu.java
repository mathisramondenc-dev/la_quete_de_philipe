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

/**
 * Fenêtre de la partie : elle fait tourner la boucle de jeu et affiche le
 * résultat.
 *
 * Boucle : un Timer Swing appelle actionPerformed() toutes les DUREE_TIC ms
 * (25 ticks/s). Chaque tick lit les touches (ClavierSouris), fait avancer le
 * Jeu, puis dessine. Tout se passe sur le thread Swing, donc pas de souci
 * d'accès concurrent avec l'affichage.
 *
 * Rendu : on dessine toujours dans une image fixe de 1920x1056 (le
 * framebuffer, taille de la carte), puis cette image est mise à l'échelle
 * dans la fenêtre en gardant ses proportions (bandes noires si besoin). Le
 * jeu ne dépend donc pas de la résolution de l'écran ; les clics souris
 * sont reconvertis dans ce même repère.
 *
 * La fenêtre gère aussi ce qui entoure le Jeu : écran de transition entre
 * niveaux (jeu en pause), sons de mort et de fin de niveau, écran de
 * victoire, plein écran (F11) et affichage des autres joueurs en
 * multijoueur. Sans Jeu (bouton Debug du menu), elle affiche l'état des
 * touches et de la souris pour tester les entrées.
 */
public class FenetreDeJeu extends Fenetre implements ActionListener {

    /** Durée d'un tick en ms (25 ticks par seconde). */
    public static final int DUREE_TIC = 40;
    public static final int IMAGES_PAR_SECONDE_DEBUG = 24;
    /** Taille de l'image de jeu : 60 x 33 tuiles de 32 px. */
    public static final int LARGEUR_JEU = 60 * 32;
    public static final int HAUTEUR_JEU = 33 * 32;
    public static final String CHEMIN_SON_MORT = "sons/mort.wav";
    public static final String CHEMIN_SON_NIVEAU = "sons/niveau.wav";

    /** Libellés de l'écran de debug, dans l'ordre des indices de ClavierSouris. */
    private static final String[] NOMS_BOUTONS = {
        "Haut (Z / flèche haut)",
        "Bas (S / flèche bas)",
        "Gauche (Q / flèche gauche)",
        "Droite (D / flèche droite)",
        "Philippe (1)",
        "Cyril (2)",
        "Norbert (3)",
        "Compétence (E)",
        "Sauter (Espace)",
        "Chef 4 (4)"
    };

    private final Jeu jeu; // null en mode debug
    private final InfosPartie infosPartie; // null en solo
    private final JPanel ecran;
    /** Image fixe dans laquelle tout est dessiné avant mise à l'échelle. */
    private final BufferedImage framebuffer;
    private final Graphics2D contexte;
    private final ClavierSouris clavierSouris;
    private final Timer timer;
    /** Échanges avec la base sur un thread séparé ; null en solo. */
    private final SynchronisationBDD synchronisationBDD;
    private boolean pleinEcran;
    private Rectangle limitesFenetreAvantPleinEcran;
    private int etatFenetreAvantPleinEcran;

    private boolean[] etatBoutons = new boolean[ClavierSouris.NB_BOUTONS];
    private long numeroTic = 0;

    private final EcranTransition ecranTransition = new EcranTransition();
    private final Son sonMort;
    private final Son sonNiveau;
    // États du tick précédent, pour ne réagir qu'aux changements
    private int niveauPrecedent;
    private boolean joueurEtaitMort = false;

    // Mesure des images par seconde (écran de debug)
    private long debutBoucle;
    private long debutSeconde;
    private int imagesDansLaSeconde = 0;
    private int imagesParSecondeMesurees = 0;

    /**
     * Fenêtre de partie solo.
     *
     * @param jeu partie à afficher, ou null pour l'écran de debug
     */
    public FenetreDeJeu(Jeu jeu) {
        this(jeu, null);
    }

    /**
     * Construit la fenêtre, l'image de rendu et branche clavier, souris et
     * F11. La boucle ne démarre qu'à l'appel de demarrer().
     *
     * @param infosPartie connexion multijoueur (avec session), ou null en solo ;
     *                    le pseudo et la partie sont alors affichés à l'écran
     */
    public FenetreDeJeu(Jeu jeu, InfosPartie infosPartie) {
        super();
        this.jeu = jeu;
        this.infosPartie = infosPartie;
        this.synchronisationBDD = (jeu != null && infosPartie != null && infosPartie.getSessionId() != null)
                ? new SynchronisationBDD(jeu, infosPartie) : null;
        if (infosPartie != null) {
            setTitle(TITRE + " - " + infosPartie.getPseudo()
                    + " - partie " + infosPartie.getIdentifiantPartie());
        }

        // Panneau qui recopie le framebuffer, agrandi et centré
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

        clavierSouris = new ClavierSouris();
        addKeyListener(clavierSouris);
        ConvertisseurSouris convertisseurSouris = new ConvertisseurSouris();
        ecran.addMouseListener(convertisseurSouris);
        ecran.addMouseMotionListener(convertisseurSouris);

        ecran.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "basculerPleinEcran");
        ecran.getActionMap().put("basculerPleinEcran", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evenement) {
                basculerPleinEcran();
            }
        });

        timer = new Timer(DUREE_TIC, this);
        sonMort = new Son(CHEMIN_SON_MORT);
        sonNiveau = new Son(CHEMIN_SON_NIVEAU);
        niveauPrecedent = (jeu != null) ? jeu.getNumeroNiveau() : 0;
        // À la fermeture : arrêt de la boucle et retrait du joueur de la partie en ligne
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { arreter(); }
        });
        setResizable(true);
        pack();
        setLocationRelativeTo(null);
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    /** @return zone de la fenêtre où dessiner le framebuffer : la plus grande possible, centrée, proportions gardées */
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

    /**
     * Alterne entre fenêtre maximisée et vrai plein écran sans bordure (F11).
     * Swing impose de masquer la fenêtre (dispose) avant de retirer ou remettre
     * ses bordures.
     */
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

    /**
     * Intermédiaire entre la souris et ClavierSouris : convertit la position
     * du clic (pixels de la fenêtre) en position dans l'image du jeu, puis
     * transmet l'événement.
     */
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

    /**
     * Démarre la boucle (et la synchronisation en multijoueur). Prend le
     * focus clavier, sans lequel aucune touche n'est reçue.
     */
    public void demarrer() {
        requestFocus();
        debutBoucle = System.currentTimeMillis();
        debutSeconde = debutBoucle;
        if (jeu == null) {
            timer.setDelay(1000 / IMAGES_PAR_SECONDE_DEBUG);
        }
        timer.start();
        if (synchronisationBDD != null) synchronisationBDD.demarrer();
    }

    /** Debug : recalcule le délai du Timer pour tenir exactement IMAGES_PAR_SECONDE_DEBUG images/s. */
    private void reglerProchainTicDebug() {
        long prochaineImage = debutBoucle + (numeroTic + 1) * 1000 / IMAGES_PAR_SECONDE_DEBUG;
        long attente = prochaineImage - System.currentTimeMillis();
        timer.setDelay((int) Math.max(1, attente));
    }

    private void mesurerImagesParSeconde() {
        imagesDansLaSeconde++;
        long maintenant = System.currentTimeMillis();
        if (maintenant - debutSeconde >= 1000) {
            imagesParSecondeMesurees = imagesDansLaSeconde;
            imagesDansLaSeconde = 0;
            debutSeconde += 1000;
        }
    }

    /** Arrête la boucle, libère les sons et quitte la partie en ligne. */
    public void arreter() {
        timer.stop();
        sonMort.fermer();
        sonNiveau.fermer();
        if (synchronisationBDD != null) synchronisationBDD.arreter();
    }

    /**
     * Un tick, appelé par le Timer. Selon l'état :
     * - transition en cours : le jeu est figé, seul l'écran de transition avance ;
     * - partie en cours : mise à jour du Jeu, détection de la mort et du
     *   changement de niveau (qui lance la transition), puis rendu ;
     * - victoire : son, arrêt de la boucle et écran « BRAVO ! » ;
     * - pas de Jeu : écran de debug.
     */
    @Override
    public void actionPerformed(ActionEvent evenement) {
        numeroTic++;
        boolean clic = clavierSouris.isClicSouris();
        etatBoutons = clavierSouris.getEtatBoutons();

        if (jeu != null && ecranTransition.estActive()) {
            ecranTransition.avancer(etatBoutons[ClavierSouris.BOUTON_SAUTER]);
            jeu.rendu(contexte);
            ecranTransition.dessiner(contexte, LARGEUR_JEU, HAUTEUR_JEU);
        } else if (jeu != null) {
            if (!jeu.isVictoire()) {
                jeu.miseAJour(etatBoutons);
                verifierMortDuJoueur();
                if (verifierChangementDeNiveau()) {
                    ecranTransition.dessiner(contexte, LARGEUR_JEU, HAUTEUR_JEU);
                    ecran.repaint();
                    return;
                }
            }
            if (jeu.isVictoire()) {
                sonNiveau.jouer();
                timer.stop();
                dessinerVictoire();
            } else {
                jeu.rendu(contexte);
                jeu.dessinerIngredients(contexte);
                jeu.getInventaireRecette().dessiner(contexte, LARGEUR_JEU);
                if (synchronisationBDD != null) GestionBDD.dessinerDistants(contexte, jeu, synchronisationBDD.getJoueursDistants());
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

    /** Joue le son de mort une seule fois, au tick où le joueur meurt (il reste mort plusieurs ticks). */
    private void verifierMortDuJoueur() {
        boolean joueurMort = jeu.getJoueur().estMort();
        if (joueurMort && !joueurEtaitMort) {
            sonMort.jouer();
        }
        joueurEtaitMort = joueurMort;
    }

    /** @return true si le numéro de niveau vient de changer ; l'écran de transition et le son sont alors lancés */
    private boolean verifierChangementDeNiveau() {
        int niveauActuel = jeu.getNumeroNiveau();
        if (niveauActuel == niveauPrecedent) {
            return false;
        }
        int niveauTermine = niveauPrecedent;
        niveauPrecedent = niveauActuel;
        ecranTransition.demarrer(niveauTermine, niveauActuel);
        sonNiveau.jouer();
        return true;
    }

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

    /** Encadré en haut à gauche : pseudo et code de la partie. */
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

    /** @return copie de l'état des boutons lu au dernier tick */
    public boolean[] getEtatBoutons() {
        return etatBoutons.clone();
    }

    /** Écran de debug : numéro de tick, images/s, souris et état de chaque bouton. */
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
