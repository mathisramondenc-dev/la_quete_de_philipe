// FenetreMenu.java
package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/* ________________________________________________________________
 * ACTION : Menu d'accueil avec le titre et le choix du mode de jeu.
 * [INFO] En haut : "Se connecter à une partie" (multijoueur).
 *        En bas : lancement direct d'un niveau ou de l'écran de
 *        debug, pratique pour tester sans passer par la connexion.
 * [INFO] Le menu s'ouvre en plein écran (fenêtre maximisée).
 * [INFO] L'image de fond (les 4 chefs) est CHEMIN_FOND. Elle remplit
 *        tout l'écran sans être déformée (les bords sont rognés si
 *        l'écran n'est pas en 16:9). Si elle est absente, le fond
 *        reste uni (COULEUR_FOND).
 * [DEPENDANCE] Main ouvre cette fenêtre. Elle ouvre FenetreHeberger,
 *              FenetreRejoindre ou directement FenetreDeJeu.
 * ________________________________________________________________ */
public class FenetreMenu extends Fenetre {

    public static final String CHEMIN_FOND = "images/fond_menu.png";

    // Les tailles du titre et des boutons sont prévues pour un écran
    // 1920x1080 et adaptées à l'écran réel (petit écran = plus petits)
    private static final double HAUTEUR_REFERENCE = 1080.0;
    private final double facteur;

    /* ________________________________________________________________
     * ACTION : Construit le menu.
     * ________________________________________________________________ */
    public FenetreMenu() {
        super();
        int hauteurEcran = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds().height;
        facteur = Math.max(0.6, Math.min(1.5, hauteurEcran / HAUTEUR_REFERENCE));

        final BufferedImage imageFond = chargerImageFond();
        JPanel fond = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphismes) {
                super.paintComponent(graphismes);
                if (imageFond != null) {
                    dessinerFond((Graphics2D) graphismes, imageFond, getWidth(), getHeight());
                }
            }
        };
        fond.setPreferredSize(new Dimension(1280, 720)); // taille si on quitte le plein écran
        fond.setBackground(COULEUR_FOND);

        JLabel titre = creerTexte(TITRE, t(64), true);
        titre.setHorizontalAlignment(JLabel.CENTER);
        titre.setBorder(BorderFactory.createEmptyBorder(t(70), 0, 0, 0));
        fond.add(titre, BorderLayout.NORTH);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        GridBagConstraints placement = new GridBagConstraints();

        // Ligne 1 : multijoueur
        placement.gridy = 0;
        placement.insets = new Insets(0, t(15), t(40), t(15));
        // === MULTIJOUEUR === Hébergement et entrée dans une partie via MariaDB.
        JPanel multijoueur = new JPanel(new GridBagLayout());
        multijoueur.setOpaque(false);
        GridBagConstraints cMulti = new GridBagConstraints();
        cMulti.insets = new Insets(0, t(15), 0, t(15));
        multijoueur.add(creerBouton("Héberger", t(300), t(80), t(28),
                e -> ouvrir(new FenetreHeberger())), cMulti);
        cMulti.gridx = 1;
        multijoueur.add(creerBouton("Rejoindre", t(300), t(80), t(28),
                e -> ouvrir(new FenetreRejoindre())), cMulti);
        centre.add(multijoueur, placement);

        // Ligne 2 : tests rapides (sans multijoueur)
        JPanel tests = new JPanel(new GridBagLayout());
        tests.setOpaque(false);
        GridBagConstraints placementTest = new GridBagConstraints();
        placementTest.insets = new Insets(0, t(6), 0, t(6));
        tests.add(creerTexte("Test :", t(18), false), placementTest);
        tests.add(creerBouton("Niveau 1", t(140), t(42), t(17), e -> lancerPartie(new Jeu("niveau1.txt"))), placementTest);
        tests.add(creerBouton("Niveau 2", t(140), t(42), t(17), e -> lancerPartie(new Jeu("niveau2.txt"))), placementTest);
        tests.add(creerBouton("Niveau 3", t(140), t(42), t(17), e -> lancerPartie(new Jeu("niveau3.txt"))), placementTest);
        tests.add(creerBouton("Debug", t(140), t(42), t(17), e -> lancerPartie(null)), placementTest);

        placement.gridy = 1;
        placement.insets = new Insets(0, 0, 0, 0);
        centre.add(tests, placement);

        fond.add(centre, BorderLayout.CENTER);

        setContentPane(fond);
        finaliserFenetre();
        // Plein écran : taille de l'écran (sans la barre des tâches), puis maximisée
        setResizable(true);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    /* ________________________________________________________________
     * ACTION : Adapte une taille (prévue pour 1920x1080) à l'écran réel.
     * ________________________________________________________________ */
    private int t(int taille) {
        return (int) Math.round(taille * facteur);
    }

    /* ________________________________________________________________
     * ACTION : Dessine l'image pour qu'elle couvre tout le panneau en
     *          gardant ses proportions (centrée, bords rognés si besoin).
     * [INFO] Pixels nets (pas de flou) car c'est du pixel art.
     * ________________________________________________________________ */
    private static void dessinerFond(Graphics2D g, BufferedImage image,
            int largeur, int hauteur) {
        double echelle = Math.max((double) largeur / image.getWidth(),
                (double) hauteur / image.getHeight());
        int largeurImage = (int) Math.ceil(image.getWidth() * echelle);
        int hauteurImage = (int) Math.ceil(image.getHeight() * echelle);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, (largeur - largeurImage) / 2, (hauteur - hauteurImage) / 2,
                largeurImage, hauteurImage, null);
    }

    /* ________________________________________________________________
     * ACTION : Charge l'image de fond du menu.
     * [INFO] Retourne null (fond uni) si l'image est absente.
     * ________________________________________________________________ */
    private static BufferedImage chargerImageFond() {
        try {
            return ImageIO.read(new File(CHEMIN_FOND));
        } catch (Exception erreur) {
            System.err.println("Fond du menu non chargé (" + CHEMIN_FOND + ") : "
                    + erreur.getMessage());
            return null;
        }
    }

    /* ________________________________________________________________
     * ACTION : Ouvre une autre fenêtre (connexion) et ferme le menu.
     * ________________________________________________________________ */
    private void ouvrir(Fenetre fenetre) {
        fenetre.setVisible(true);
        dispose();
    }

    /* ________________________________________________________________
     * ACTION : Ferme le menu et ouvre la fenêtre de jeu.
     * [INFO] Sans Jeu (null), la fenêtre affiche l'écran de debug.
     * ________________________________________________________________ */
    private void lancerPartie(Jeu jeu) {
        FenetreDeJeu fenetreDeJeu = new FenetreDeJeu(jeu);
        fenetreDeJeu.setVisible(true);
        fenetreDeJeu.demarrer();
        dispose();
    }
}
