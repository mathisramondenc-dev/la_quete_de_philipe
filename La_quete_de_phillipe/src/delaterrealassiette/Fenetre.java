// Fenetre.java
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

/* ________________________________________________________________
 * ACTION : Base commune des fenêtres du jeu (titre, taille, fermeture,
 *          couleurs, image de fond et petits outils pour les formulaires).
 * [INFO] L'image de fond (les 4 chefs) s'appelle NOM_IMAGE_FOND. Elle est
 *        cherchée d'abord à côté des fichiers .java (src/delaterrealassiette),
 *        puis dans le dossier images/. Si elle est introuvable, le fond
 *        reste uni (COULEUR_FOND) et le chemin cherché est écrit en console.
 * [INFO] Les fenêtres de l'interface s'ouvrent en plein écran : les tailles
 *        (polices, boutons) sont prévues pour 1920x1080 et adaptées à
 *        l'écran réel avec t(...).
 * [DEPENDANCE] FenetreMenu, FenetreHeberger, FenetreRejoindre,
 *              FenetreConnexion, FenetreControles et FenetreDeJeu en héritent.
 * ________________________________________________________________ */
public abstract class Fenetre extends JFrame {

    public static final String TITRE = "La Quête de Philippe";
    public static final int LARGEUR = 800;
    public static final int HAUTEUR = 320;

    // Couleurs communes à toutes les fenêtres
    public static final Color COULEUR_FOND = new Color(40, 30, 25);
    public static final Color COULEUR_TEXTE = Color.WHITE;

    public static final String NOM_IMAGE_FOND = "fond_menu.png";
    private static BufferedImage imageFond;          // chargée une seule fois
    private static boolean imageFondDejaCherchee = false;
    private static final int HAUTEUR_MUR_POURCENT = 45; // mur sans les chefs

    // Adaptation des tailles à l'écran (1 pour un écran 1920x1080)
    private static final double HAUTEUR_REFERENCE = 1080.0;
    private final double facteur;

    /* ________________________________________________________________
     * ACTION : Configure la fenêtre sans l'afficher.
     * [INFO] Appeler setVisible(true) après la construction.
     * ________________________________________________________________ */
    public Fenetre() {
        super(TITRE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        int hauteurEcran = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds().height;
        facteur = Math.max(0.6, Math.min(1.5, hauteurEcran / HAUTEUR_REFERENCE));
    }

    /* ________________________________________________________________
     * ACTION : Adapte une taille prévue pour un écran 1920x1080 à l'écran
     *          réel (plus petit sur un petit écran, plus grand sur un grand).
     * ________________________________________________________________ */
    protected int t(int taille) {
        return (int) Math.round(taille * facteur);
    }

    /* ________________________________________________________________
     * ACTION : Met la fenêtre en plein écran (taille de l'écran sans la
     *          barre des tâches, puis maximisée).
     * [INFO] À appeler à la fin du constructeur, après finaliserFenetre().
     * ________________________________________________________________ */
    protected void passerEnPleinEcran() {
        setResizable(true);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
    }

    /* ________________________________________________________________
     * ACTION : Construit un écran plein écran : fond avec les 4 chefs,
     *          titre du jeu en haut, et au centre une carte sombre avec
     *          un sous-titre et le contenu (formulaire, liste...).
     * [DEPENDANCE] Utilisé par FenetreHeberger, FenetreRejoindre,
     *              FenetreConnexion et FenetreControles.
     * ________________________________________________________________ */
    protected JPanel creerEcranPleinEcran(String sousTitre, JComponent contenu) {
        JPanel fond = creerPanneauFond(true);
        fond.setPreferredSize(new Dimension(1280, 720)); // si on quitte le plein écran

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

        JPanel centre = new JPanel(new GridBagLayout()); // centre la carte
        centre.setOpaque(false);
        centre.add(carte);
        fond.add(centre, BorderLayout.CENTER);
        return fond;
    }

    /* ________________________________________________________________
     * ACTION : Crée une carte sombre semi-transparente aux coins arrondis,
     *          pour que le texte reste lisible par-dessus l'image de fond.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Ajuste la taille au contenu et centre la fenêtre à l'écran.
     * [INFO] À appeler à la fin du constructeur des classes filles.
     * ________________________________________________________________ */
    protected void finaliserFenetre() {
        pack();
        setLocationRelativeTo(null);
    }

    /* ________________________________________________________________
     * ACTION : Crée un bouton avec un texte, une taille et une action.
     * ________________________________________________________________ */
    protected JButton creerBouton(String texte, int largeur, int hauteur,
            int taillePolice, ActionListener action) {
        JButton bouton = new JButton(texte);
        bouton.setFont(new Font("SansSerif", Font.BOLD, taillePolice));
        bouton.setPreferredSize(new Dimension(largeur, hauteur));
        bouton.addActionListener(action);
        return bouton;
    }

    /* ________________________________________________________________
     * ACTION : Crée un texte blanc pour les fonds sombres.
     * ________________________________________________________________ */
    protected JLabel creerTexte(String texte, int taillePolice, boolean gras) {
        JLabel label = new JLabel(texte);
        label.setFont(new Font("SansSerif", gras ? Font.BOLD : Font.PLAIN, taillePolice));
        label.setForeground(COULEUR_TEXTE);
        return label;
    }

    /* ________________________________________________________________
     * ACTION : Ajoute une ligne "intitulé : champ" dans un formulaire.
     * [INFO] Le panneau doit utiliser un GridBagLayout.
     * [INFO] Les champs de texte sont agrandis selon la taille de l'écran.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Crée le panneau principal d'une fenêtre, avec l'image de
     *          fond qui remplit tout le panneau sans être déformée.
     * [INFO] À utiliser à la place de new JPanel(new BorderLayout()).
     * [INFO] avecChefs = false : seulement le haut de l'image (le mur de
     *        briques), pour les petites fenêtres avec un formulaire.
     * ________________________________________________________________ */
    protected JPanel creerPanneauFond(final boolean avecChefs) {
        JPanel panneau = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphismes) {
                super.paintComponent(graphismes);
                BufferedImage image = getImageFond();
                if (image != null && !avecChefs) {
                    // Haut de l'image seulement : le mur, au-dessus des chefs
                    image = image.getSubimage(0, 0, image.getWidth(),
                            image.getHeight() * HAUTEUR_MUR_POURCENT / 100);
                }
                if (image != null) {
                    dessinerImageCouvrante((Graphics2D) graphismes, image, getWidth(), getHeight());
                }
            }
        };
        panneau.setBackground(COULEUR_FOND); // si l'image est absente
        return panneau;
    }

    /* ________________________________________________________________
     * ACTION : Retourne l'image de fond (null si introuvable).
     * [INFO] 1) à côté des .java (ressource du package delaterrealassiette)
     *        2) sinon images/fond_menu.png dans le dossier du projet
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Dessine l'image pour qu'elle couvre toute la zone en gardant
     *          ses proportions (centrée, bords rognés si besoin).
     * [INFO] Pixels nets (pas de flou) car c'est du pixel art.
     * ________________________________________________________________ */
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
