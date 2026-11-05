// FenetreRejoindre.java
package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/* ________________________________________________________________
 * ACTION : Fenêtre pour rejoindre une partie hébergée par un autre
 *          joueur : pseudo, identifiant de la partie et mot de passe.
 * [INFO] Les champs sont vérifiés avant de lancer la partie, avec un
 *        message d'erreur (JOptionPane) si quelque chose ne va pas.
 * [DEPENDANCE] Ouverte par FenetreMenu, ouvre FenetreDeJeu.
 *              Plus tard : vérifier la partie dans la BDD (voir TODO).
 * ________________________________________________________________ */
public class FenetreRejoindre extends Fenetre {

    // Niveau utilisé tant que la BDD ne donne pas celui choisi par l'hôte
    private static final String NIVEAU_PAR_DEFAUT = "niveau1.txt";

    private final JTextField champPseudo;
    private final JTextField champIdentifiant;
    private final JPasswordField champMotDePasse;

    /* ________________________________________________________________
     * ACTION : Construit le formulaire pour rejoindre une partie.
     * ________________________________________________________________ */
    public FenetreRejoindre() {
        super();

        JPanel fond = new JPanel(new BorderLayout());
        fond.setBackground(COULEUR_FOND);
        fond.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titre = creerTexte("Rejoindre une partie", 30, true);
        titre.setHorizontalAlignment(JLabel.CENTER);
        fond.add(titre, BorderLayout.NORTH);

        // ----- Formulaire -----
        champPseudo = new JTextField(15);
        champIdentifiant = new JTextField(15);
        champMotDePasse = new JPasswordField(15);

        // L'identifiant est toujours en majuscules (k7px3m -> K7PX3M)
        champIdentifiant.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evenement) {
                String texte = champIdentifiant.getText();
                if (!texte.equals(texte.toUpperCase())) {
                    int curseur = champIdentifiant.getCaretPosition();
                    champIdentifiant.setText(texte.toUpperCase());
                    champIdentifiant.setCaretPosition(curseur);
                }
            }
        });

        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setOpaque(false);
        ajouterLigneFormulaire(formulaire, 0, "Pseudo :", champPseudo);
        ajouterLigneFormulaire(formulaire, 1, "Identifiant de la partie :", champIdentifiant);
        ajouterLigneFormulaire(formulaire, 2, "Mot de passe de la partie :", champMotDePasse);
        fond.add(formulaire, BorderLayout.CENTER);

        // ----- Boutons -----
        JButton boutonRejoindre = creerBouton("Rejoindre", 200, 45, 18, e -> rejoindrePartie());
        JButton boutonRetour = creerBouton("Retour", 120, 45, 16, e -> retourMenu());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        boutons.setOpaque(false);
        boutons.add(boutonRetour);
        boutons.add(boutonRejoindre);
        fond.add(boutons, BorderLayout.SOUTH);

        setContentPane(fond);
        getRootPane().setDefaultButton(boutonRejoindre); // Entrée = Rejoindre
        setMinimumSize(new Dimension(560, 0));
        finaliserFenetre();
    }

    /* ________________________________________________________________
     * ACTION : Vérifie les champs, puis rejoint la partie et ouvre le jeu.
     * [INFO] En cas d'erreur, un message s'affiche et on reste ici.
     * ________________________________________________________________ */
    private void rejoindrePartie() {
        String pseudo = champPseudo.getText().trim();
        String erreur = InfosPartie.verifierPseudo(pseudo);
        if (erreur != null) {
            afficherErreur(erreur);
            champPseudo.requestFocus();
            return;
        }

        String identifiant = champIdentifiant.getText().trim().toUpperCase();
        erreur = InfosPartie.verifierIdentifiant(identifiant);
        if (erreur != null) {
            afficherErreur(erreur);
            champIdentifiant.requestFocus();
            return;
        }

        String motDePasse = new String(champMotDePasse.getPassword());
        InfosPartie infos = InfosPartie.pourInvite(pseudo, identifiant, motDePasse);

        // TODO BDD : vérifier que la partie existe, que le mot de passe
        //            est correct, qu'il reste une place et que le pseudo
        //            n'est pas déjà pris. Si non : afficherErreur(...) et return.
        //            Récupérer aussi le niveau choisi par l'hôte.
        System.out.println("Partie rejointe : " + infos);

        lancerPartie(infos);
    }

    /* ________________________________________________________________
     * ACTION : Ouvre la fenêtre de jeu et ferme celle-ci.
     * ________________________________________________________________ */
    private void lancerPartie(InfosPartie infos) {
        FenetreDeJeu fenetreDeJeu = new FenetreDeJeu(new Jeu(NIVEAU_PAR_DEFAUT), infos);
        fenetreDeJeu.setVisible(true);
        fenetreDeJeu.demarrer();
        dispose();
    }

    /* ________________________________________________________________
     * ACTION : Revient au menu principal.
     * ________________________________________________________________ */
    private void retourMenu() {
        new FenetreMenu().setVisible(true);
        dispose();
    }

    private void afficherErreur(String message) {
        JOptionPane.showMessageDialog(this, message, "Impossible de rejoindre la partie",
                JOptionPane.ERROR_MESSAGE);
    }
}
