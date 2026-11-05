// FenetreMenu.java
package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/* ________________________________________________________________
 * ACTION : Menu d'accueil avec le titre et le choix du mode de jeu.
 * [INFO] Un clic sur un bouton ferme le menu et ouvre la fenêtre de jeu,
 *        sur un des niveaux ou sur l'écran de debug.
 * [DEPENDANCE] Main ouvre cette fenêtre, elle ouvre FenetreDeJeu.
 * ________________________________________________________________ */
public class FenetreMenu extends Fenetre {

    /* ________________________________________________________________
     * ACTION : Construit le menu.
     * ________________________________________________________________ */
    public FenetreMenu() {
        super();

        JPanel fond = new JPanel(new BorderLayout());
        fond.setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        fond.setBackground(new Color(40, 30, 25));

        JLabel titre = new JLabel(TITRE, JLabel.CENTER);
        titre.setFont(new Font("SansSerif", Font.BOLD, 40));
        titre.setForeground(Color.WHITE);
        titre.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        fond.add(titre, BorderLayout.NORTH);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        GridBagConstraints placement = new GridBagConstraints();
        placement.insets = new Insets(0, 10, 0, 10);
        centre.add(creerBouton("Niveau 1", e -> lancerPartie(new Jeu("niveau1.txt"))), placement);
        centre.add(creerBouton("Niveau 2", e -> lancerPartie(new Jeu("niveau2.txt"))), placement);
        centre.add(creerBouton("Debug", e -> lancerPartie(null)), placement);
        fond.add(centre, BorderLayout.CENTER);

        setContentPane(fond);
        finaliserFenetre();
    }

    /* ________________________________________________________________
     * ACTION : Crée un bouton du menu.
     * ________________________________________________________________ */
    private JButton creerBouton(String texte, ActionListener action) {
        JButton bouton = new JButton(texte);
        bouton.setFont(new Font("SansSerif", Font.BOLD, 24));
        bouton.setPreferredSize(new Dimension(200, 70));
        bouton.addActionListener(action);
        return bouton;
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
