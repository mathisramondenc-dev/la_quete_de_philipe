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
 * [INFO] En haut : Héberger / Rejoindre (multijoueur), puis Contrôles.
 *        En bas : lancement direct d'un niveau ou de l'écran de
 *        debug, pratique pour tester sans passer par la connexion.
 * [INFO] Le menu s'ouvre en plein écran (fenêtre maximisée).
 * [INFO] L'image de fond (les 4 chefs) est gérée par Fenetre : elle
 *        remplit tout l'écran sans être déformée.
 * [DEPENDANCE] Main ouvre cette fenêtre. Elle ouvre FenetreHeberger,
 *              FenetreRejoindre, FenetreControles ou directement FenetreDeJeu.
 * ________________________________________________________________ */
public class FenetreMenu extends Fenetre {

    /* ________________________________________________________________
     * ACTION : Construit le menu.
     * ________________________________________________________________ */
    public FenetreMenu() {
        super();

        JPanel fond = creerPanneauFond(true); // image des 4 chefs (voir Fenetre)
        fond.setPreferredSize(new Dimension(1280, 720)); // taille si on quitte le plein écran

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

        // Ligne 2 : écran des contrôles (touches du jeu)
        placement.gridy = 1;
        placement.insets = new Insets(0, t(15), t(40), t(15));
        centre.add(creerBouton("Contrôles", t(300), t(60), t(24),
                e -> ouvrir(new FenetreControles())), placement);

        // Ligne 3 : tests rapides (sans multijoueur)
        JPanel tests = new JPanel(new GridBagLayout());
        tests.setOpaque(false);
        GridBagConstraints placementTest = new GridBagConstraints();
        placementTest.insets = new Insets(0, t(6), 0, t(6));
        tests.add(creerTexte("Test :", t(18), false), placementTest);
        tests.add(creerBouton("Niveau 1", t(140), t(42), t(17), e -> lancerPartie(new Jeu("niveau1.txt"))), placementTest);
        tests.add(creerBouton("Niveau 2", t(140), t(42), t(17), e -> lancerPartie(new Jeu("niveau2.txt"))), placementTest);
        tests.add(creerBouton("Niveau 3", t(140), t(42), t(17), e -> lancerPartie(new Jeu("niveau3.txt"))), placementTest);
        tests.add(creerBouton("Debug", t(140), t(42), t(17), e -> lancerPartie(null)), placementTest);

        placement.gridy = 2;
        placement.insets = new Insets(0, 0, 0, 0);
        centre.add(tests, placement);

        fond.add(centre, BorderLayout.CENTER);

        setContentPane(fond);
        finaliserFenetre();
        passerEnPleinEcran();
    }


    /* ________________________________________________________________
     * ACTION : Ouvre une autre fenêtre et ferme le menu.
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
