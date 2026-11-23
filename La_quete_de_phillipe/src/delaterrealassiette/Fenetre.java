// Fenetre.java
package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/* ________________________________________________________________
 * ACTION : Base commune des fenêtres du jeu (titre, taille, fermeture,
 *          couleurs, image de fond et petits outils pour les formulaires).
 * [INFO] L'image de fond (les 4 chefs) s'appelle NOM_IMAGE_FOND. Elle est
 *        cherchée d'abord à côté des fichiers .java (src/delaterrealassiette),
 *        puis dans le dossier images/. Si elle est introuvable, le fond
 *        reste uni (COULEUR_FOND) et le chemin cherché est écrit en console.
 * [DEPENDANCE] FenetreMenu, FenetreConnexion et FenetreDeJeu en héritent.
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

    /* ________________________________________________________________
     * ACTION : Configure la fenêtre sans l'afficher.
     * [INFO] Appeler setVisible(true) après la construction.
     * ________________________________________________________________ */
    public Fenetre() {
        super(TITRE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
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
     * ________________________________________________________________ */
    protected void ajouterLigneFormulaire(JPanel formulaire, int ligne,
            String intitule, Component champ) {
        GridBagConstraints placement = new GridBagConstraints();
        placement.gridy = ligne;
        placement.insets = new Insets(6, 6, 6, 6);

        placement.gridx = 0;
        placement.anchor = GridBagConstraints.EAST;
        formulaire.add(creerTexte(intitule, 16, false), placement);

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
