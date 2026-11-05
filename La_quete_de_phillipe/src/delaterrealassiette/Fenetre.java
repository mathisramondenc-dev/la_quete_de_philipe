// Fenetre.java
package delaterrealassiette;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/* ________________________________________________________________
 * ACTION : Base commune des fenêtres du jeu (titre, taille, fermeture,
 *          couleurs et petits outils pour construire les formulaires).
 * [DEPENDANCE] FenetreMenu, FenetreConnexion et FenetreDeJeu en héritent.
 * ________________________________________________________________ */
public abstract class Fenetre extends JFrame {

    public static final String TITRE = "La Quête de Philippe";
    public static final int LARGEUR = 800;
    public static final int HAUTEUR = 320;

    // Couleurs communes à toutes les fenêtres
    public static final Color COULEUR_FOND = new Color(40, 30, 25);
    public static final Color COULEUR_TEXTE = Color.WHITE;

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
}
