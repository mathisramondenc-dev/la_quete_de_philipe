package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

/**
 * Classe mère de toutes les fenêtres du jeu (menu, formulaires, contrôles,
 * fenêtre de jeu).
 *
 * Elle évite de répéter la même mise en forme dans chaque écran en
 * fournissant des « briques » prêtes à l'emploi : boutons, textes, lignes de
 * formulaire, carte sombre centrée, fond illustré et passage en plein écran.
 * Un écran de formulaire se construit donc en quelques lignes avec
 * creerEcranPleinEcran(sousTitre, contenu).
 *
 * Adaptation à l'écran : toutes les tailles sont pensées pour un écran
 * 1920x1080 et passent par t(...), qui les met à l'échelle de l'écran réel.
 *
 * L'image de fond (les 4 chefs) est chargée une seule fois et partagée ; si
 * elle est introuvable, le fond reste uni et un message l'indique en console.
 */
public abstract class Fenetre extends JFrame {

    public static final String TITRE = "La Quête de Philippe";
    public static final int LARGEUR = 800;
    public static final int HAUTEUR = 320;

    public static final Color COULEUR_FOND = new Color(40, 30, 25);
    public static final Color COULEUR_TEXTE = Color.WHITE;

    public static final String NOM_IMAGE_FOND = "fond_menu.png";
    /** Chargée au premier besoin puis partagée par toutes les fenêtres. */
    private static BufferedImage imageFond;
    private static boolean imageFondDejaCherchee = false;
    /** Part haute de l'image (mur de briques, sans les chefs), en pourcentage. */
    private static final int HAUTEUR_MUR_POURCENT = 45;

    /** Hauteur d'écran pour laquelle les tailles sont prévues (facteur 1). */
    private static final double HAUTEUR_REFERENCE = 1080.0;
    private final double facteur;

    /** Configure la fenêtre sans l'afficher (appeler setVisible(true) ensuite). */
    public Fenetre() {
        super(TITRE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        int hauteurEcran = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds().height;
        facteur = Math.max(0.6, Math.min(1.5, hauteurEcran / HAUTEUR_REFERENCE));
    }

    /**
     * Met à l'échelle une taille prévue pour un écran 1080p (facteur borné
     * entre 0.6 et 1.5).
     *
     * @param taille taille en pixels sur un écran 1920x1080
     * @return taille adaptée à l'écran réel
     */
    protected int t(int taille) {
        return (int) Math.round(taille * facteur);
    }

    /** Agrandit la fenêtre à tout l'écran (hors barre des tâches). À appeler après finaliserFenetre(). */
    protected void passerEnPleinEcran() {
        setResizable(true);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    /**
     * Construit un écran complet : fond illustré, titre du jeu en haut et,
     * au centre, une carte sombre avec un sous-titre et le contenu fourni.
     *
     * @param contenu formulaire ou liste à placer dans la carte
     * @return panneau à passer à setContentPane()
     */
    protected JPanel creerEcranPleinEcran(String sousTitre, JComponent contenu) {
        JPanel fond = creerPanneauFond(true);
        fond.setPreferredSize(new Dimension(1280, 720)); // taille hors plein écran

        JLabel titre = creerTexte(TITRE, t(64), true);
        titre.setHorizontalAlignment(JLabel.CENTER);
        titre.setBorder(BorderFactory.createEmptyBorder(t(60), 0, 0, 0));
        fond.add(titre, BorderLayout.NORTH);

        JPanel carte = creerCarte();
        JLabel texteSousTitre = creerTexte(sousTitre, t(36), true);
        texteSousTitre.setHorizontalAlignment(JLabel.CENTER);
        texteSousTitre.setBorder(BorderFactory.createEmptyBorder(0, 0, t(20), 0));
        carte.add(texteSousTitre, BorderLayout.NORTH);
        carte.add(contenu, BorderLayout.CENTER);

        JPanel centre = new JPanel(new GridBagLayout()); // GridBagLayout seul = centrage
        centre.setOpaque(false);
        centre.add(carte);
        fond.add(centre, BorderLayout.CENTER);
        return fond;
    }

    /** Panneau sombre semi-transparent aux coins arrondis, pour garder le texte lisible sur le fond. */
    protected JPanel creerCarte() {
        JPanel carte = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphismes) {
                Graphics2D g = (Graphics2D) graphismes.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int arrondi = t(24);
                g.setColor(new Color(15, 10, 8, 200));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), arrondi, arrondi);
                g.setColor(new Color(255, 220, 95, 120));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arrondi, arrondi);
                g.dispose();
            }
        };
        carte.setOpaque(false);
        carte.setBorder(BorderFactory.createEmptyBorder(t(30), t(45), t(30), t(45)));
        return carte;
    }

    /** Ajuste la taille au contenu et centre la fenêtre. À appeler en fin de constructeur. */
    protected void finaliserFenetre() {
        pack();
        setLocationRelativeTo(null);
    }

    protected JButton creerBouton(String texte, int largeur, int hauteur,
            int taillePolice, ActionListener action) {
        JButton bouton = new JButton(texte);
        bouton.setFont(new Font("SansSerif", Font.BOLD, taillePolice));
        bouton.setPreferredSize(new Dimension(largeur, hauteur));
        bouton.addActionListener(action);
        return bouton;
    }

    /** Texte blanc, prévu pour les fonds sombres. */
    protected JLabel creerTexte(String texte, int taillePolice, boolean gras) {
        JLabel label = new JLabel(texte);
        label.setFont(new Font("SansSerif", gras ? Font.BOLD : Font.PLAIN, taillePolice));
        label.setForeground(COULEUR_TEXTE);
        return label;
    }

    /**
     * Ajoute une ligne « intitulé : champ » à un formulaire en GridBagLayout
     * (intitulé aligné à droite, champ étiré à gauche).
     *
     * @param ligne numéro de la ligne dans le formulaire (0 en haut)
     */
    protected void ajouterLigneFormulaire(JPanel formulaire, int ligne,
            String intitule, Component champ) {
        GridBagConstraints placement = new GridBagConstraints();
        placement.gridy = ligne;
        placement.insets = new Insets(t(10), t(10), t(10), t(10));

        if (champ instanceof JTextComponent) {
            champ.setFont(new Font("SansSerif", Font.PLAIN, t(22)));
            champ.setPreferredSize(new Dimension(t(380), t(44)));
        }

        placement.gridx = 0;
        placement.anchor = GridBagConstraints.EAST;
        formulaire.add(creerTexte(intitule, t(22), false), placement);

        placement.gridx = 1;
        placement.anchor = GridBagConstraints.WEST;
        placement.fill = GridBagConstraints.HORIZONTAL;
        placement.weightx = 1;
        formulaire.add(champ, placement);
    }

    /**
     * Panneau principal d'une fenêtre, avec l'image de fond qui le couvre
     * entièrement sans déformation.
     *
     * @param avecChefs false pour n'afficher que le haut de l'image (le mur)
     */
    protected JPanel creerPanneauFond(final boolean avecChefs) {
        JPanel panneau = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphismes) {
                super.paintComponent(graphismes);
                BufferedImage image = getImageFond();
                if (image != null && !avecChefs) {
                    image = image.getSubimage(0, 0, image.getWidth(),
                            image.getHeight() * HAUTEUR_MUR_POURCENT / 100);
                }
                if (image != null) {
                    dessinerImageCouvrante((Graphics2D) graphismes, image, getWidth(), getHeight());
                }
            }
        };
        panneau.setBackground(COULEUR_FOND); // visible si l'image manque
        return panneau;
    }

    /**
     * Cherche l'image de fond une seule fois : d'abord à côté des sources
     * (ressource du package), sinon dans images/.
     *
     * @return l'image, ou null si elle est introuvable
     */
    public static BufferedImage getImageFond() {
        if (!imageFondDejaCherchee) {
            imageFondDejaCherchee = true;
            File fichier = new File("images", NOM_IMAGE_FOND);
            try {
                URL ressource = Fenetre.class.getResource(NOM_IMAGE_FOND);
                if (ressource != null) {
                    imageFond = ImageIO.read(ressource);
                } else if (fichier.exists()) {
                    imageFond = ImageIO.read(fichier);
                }
            } catch (Exception erreur) {
                imageFond = null;
            }
            if (imageFond == null) {
                System.err.println("Image de fond introuvable : mettre " + NOM_IMAGE_FOND
                        + " dans src/delaterrealassiette/ ou dans "
                        + fichier.getAbsolutePath());
            }
        }
        return imageFond;
    }

    /**
     * Dessine l'image de façon à couvrir toute la zone en gardant ses
     * proportions (centrée, bords rognés si besoin). Interpolation « plus
     * proche voisin » pour garder le pixel art net.
     */
    protected static void dessinerImageCouvrante(Graphics2D g, BufferedImage image,
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
}
