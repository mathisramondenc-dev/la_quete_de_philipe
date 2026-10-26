// FenetreMenu.java
package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/* ________________________________________________________________
 * ACTION : Menu d'accueil avec le titre et le bouton START.
 * [INFO] Un clic sur START ferme le menu et ouvre la fenêtre de jeu.
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

        JButton boutonStart = new JButton("START");
        boutonStart.setFont(new Font("SansSerif", Font.BOLD, 28));
        boutonStart.setPreferredSize(new Dimension(220, 70));
        boutonStart.addActionListener(e -> lancerPartie());

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        centre.add(boutonStart);
        fond.add(centre, BorderLayout.CENTER);

        setContentPane(fond);
        finaliserFenetre();
    }

    /* ________________________________________________________________
     * ACTION : Ferme le menu et ouvre la fenêtre de jeu.
     * [DEPENDANCE] Plus tard : créer ici le Jeu et le passer à FenetreDeJeu.
     * ________________________________________________________________ */
    private void lancerPartie() {
        FenetreDeJeu fenetreDeJeu = new FenetreDeJeu();
        fenetreDeJeu.setVisible(true);
        fenetreDeJeu.demarrer();
        dispose();
    }
}
