// FenetreMenu.java
package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
 * [DEPENDANCE] Main ouvre cette fenêtre. Elle ouvre FenetreConnexion
 *              ou directement FenetreDeJeu.
 * ________________________________________________________________ */
public class FenetreMenu extends Fenetre {

    /* ________________________________________________________________
     * ACTION : Construit le menu.
     * ________________________________________________________________ */
    public FenetreMenu() {
        super();

        JPanel fond = new JPanel(new BorderLayout());
        fond.setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        fond.setBackground(COULEUR_FOND);

        JLabel titre = creerTexte(TITRE, 40, true);
        titre.setHorizontalAlignment(JLabel.CENTER);
        titre.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        fond.add(titre, BorderLayout.NORTH);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        GridBagConstraints placement = new GridBagConstraints();

        // Ligne 1 : multijoueur
        placement.gridy = 0;
        placement.insets = new Insets(0, 15, 25, 15);
        centre.add(creerBouton("Se connecter à une partie", 420, 70, 24,
                e -> ouvrir(new FenetreConnexion())), placement);

        // Ligne 2 : tests rapides (sans multijoueur)
        JPanel tests = new JPanel(new GridBagLayout());
        tests.setOpaque(false);
        GridBagConstraints placementTest = new GridBagConstraints();
        placementTest.insets = new Insets(0, 6, 0, 6);
        tests.add(creerTexte("Test :", 14, false), placementTest);
        tests.add(creerBouton("Niveau 1", 120, 35, 14, e -> lancerPartie(new Jeu("niveau1.txt"))), placementTest);
        tests.add(creerBouton("Niveau 2", 120, 35, 14, e -> lancerPartie(new Jeu("niveau2.txt"))), placementTest);
        tests.add(creerBouton("Niveau 3", 120, 35, 14, e -> lancerPartie(new Jeu("niveau3.txt"))), placementTest);
        tests.add(creerBouton("Debug", 120, 35, 14, e -> lancerPartie(null)), placementTest);

        placement.gridy = 1;
        placement.insets = new Insets(0, 0, 0, 0);
        centre.add(tests, placement);

        fond.add(centre, BorderLayout.CENTER);

        setContentPane(fond);
        finaliserFenetre();
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
