package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Menu d'accueil, premier écran affiché au lancement.
 *
 * Trois rangées de boutons :
 * 1) multijoueur : Héberger ou Rejoindre une partie ;
 * 2) Contrôles : rappel des touches ;
 * 3) tests : lancement direct d'un niveau, en solo, ou de l'écran de debug,
 *    pour tester sans passer par la base de données.
 *
 * La navigation suit toujours le même principe : on ouvre l'écran suivant
 * puis on ferme le menu (une seule fenêtre ouverte à la fois).
 */
public class FenetreMenu extends Fenetre {

    public FenetreMenu() {
        super();

        JPanel fond = creerPanneauFond(true);
        fond.setPreferredSize(new Dimension(1280, 720)); // taille hors plein écran

        JLabel titre = creerTexte(TITRE, t(64), true);
        titre.setHorizontalAlignment(JLabel.CENTER);
        titre.setBorder(BorderFactory.createEmptyBorder(t(70), 0, 0, 0));
        fond.add(titre, BorderLayout.NORTH);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        GridBagConstraints placement = new GridBagConstraints();

        // Rangée 1 : multijoueur
        placement.gridy = 0;
        placement.insets = new Insets(0, t(15), t(40), t(15));
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

        // Rangée 2 : contrôles
        placement.gridy = 1;
        placement.insets = new Insets(0, t(15), t(40), t(15));
        centre.add(creerBouton("Contrôles", t(300), t(60), t(24),
                e -> ouvrir(new FenetreControles())), placement);

        // Rangée 3 : tests en solo
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


    /** Affiche la fenêtre donnée et ferme le menu. */
    private void ouvrir(Fenetre fenetre) {
        fenetre.setVisible(true);
        dispose();
    }

    /**
     * Lance une partie solo et ferme le menu.
     *
     * @param jeu partie à lancer, ou null pour l'écran de debug
     */
    private void lancerPartie(Jeu jeu) {
        FenetreDeJeu fenetreDeJeu = new FenetreDeJeu(jeu);
        fenetreDeJeu.setVisible(true);
        fenetreDeJeu.demarrer();
        dispose();
    }
}
