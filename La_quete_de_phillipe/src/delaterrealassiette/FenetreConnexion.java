// FenetreConnexion.java
package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/* ________________________________________________________________
 * ACTION : Fenêtre "Se connecter à une partie" : le joueur saisit son
 *          pseudo, l'identifiant de la partie et le mot de passe.
 * [INFO] Les trois champs sont obligatoires. Ils sont vérifiés avant
 *        de lancer la partie, avec un message d'erreur (JOptionPane)
 *        si quelque chose ne va pas.
 * [DEPENDANCE] Ouverte par FenetreMenu, ouvre FenetreDeJeu.
 *              Plus tard : vérifier la partie dans la BDD (voir TODO).
 * ________________________________________________________________ */
public class FenetreConnexion extends Fenetre {

    // Niveau utilisé tant que la BDD ne donne pas celui de la partie
    private static final String NIVEAU_PAR_DEFAUT = "niveau3.txt";

    private final JTextField champPseudo;
    private final JTextField champIdentifiant;
    private final JPasswordField champMotDePasse;

    /* ________________________________________________________________
     * ACTION : Construit le formulaire de connexion.
     * ________________________________________________________________ */
    public FenetreConnexion() {
        super();

        JPanel fond = new JPanel(new BorderLayout());
        fond.setBackground(COULEUR_FOND);
        fond.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titre = creerTexte("Se connecter à une partie", 30, true);
        titre.setHorizontalAlignment(JLabel.CENTER);
        fond.add(titre, BorderLayout.NORTH);

        // ----- Formulaire -----
        champPseudo = new JTextField(15);
        champIdentifiant = new JTextField(15);
        champMotDePasse = new JPasswordField(15);

        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setOpaque(false);
        ajouterLigneFormulaire(formulaire, 0, "Pseudo :", champPseudo);
        ajouterLigneFormulaire(formulaire, 1, "Identifiant de la partie :", champIdentifiant);
        ajouterLigneFormulaire(formulaire, 2, "Mot de passe :", champMotDePasse);
        fond.add(formulaire, BorderLayout.CENTER);

        // ----- Boutons -----
        JButton boutonConnexion = creerBouton("Se connecter", 200, 45, 18, e -> seConnecter());
        JButton boutonRetour = creerBouton("Retour", 120, 45, 16, e -> retourMenu());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        boutons.setOpaque(false);
        boutons.add(boutonRetour);
        boutons.add(boutonConnexion);
        fond.add(boutons, BorderLayout.SOUTH);

        setContentPane(fond);
        getRootPane().setDefaultButton(boutonConnexion); // Entrée = Se connecter
        setMinimumSize(new Dimension(560, 0));
        finaliserFenetre();
    }

    /* ________________________________________________________________
     * ACTION : Vérifie les trois champs, puis ouvre la partie.
     * [INFO] Au premier champ invalide, un message s'affiche, le curseur
     *        va dans ce champ et on reste sur cette fenêtre.
     * ________________________________________________________________ */
    private void seConnecter() {
        String pseudo = champPseudo.getText().trim();
        String identifiant = champIdentifiant.getText().trim();
        String motDePasse = new String(champMotDePasse.getPassword());

        if (!champValide(InfosPartie.verifierPseudo(pseudo), champPseudo)
                || !champValide(InfosPartie.verifierIdentifiant(identifiant), champIdentifiant)
                || !champValide(InfosPartie.verifierMotDePasse(motDePasse), champMotDePasse)) {
            return;
        }

        InfosPartie infos = new InfosPartie(pseudo, identifiant, motDePasse);

        // TODO BDD : vérifier que la partie existe, que le mot de passe
        //            est correct, qu'il reste une place et que le pseudo
        //            n'est pas déjà pris. Si non : afficherErreur(...) et return.
        //            Récupérer aussi le niveau de la partie.
        System.out.println("Connexion : " + infos);

        lancerPartie(infos);
    }

    /* ________________________________________________________________
     * ACTION : Retourne true si erreur vaut null. Sinon, affiche
     *          l'erreur, place le curseur dans le champ et retourne false.
     * ________________________________________________________________ */
    private boolean champValide(String erreur, JTextField champ) {
        if (erreur == null) {
            return true;
        }
        afficherErreur(erreur);
        champ.requestFocus();
        return false;
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
        JOptionPane.showMessageDialog(this, message, "Connexion impossible",
                JOptionPane.ERROR_MESSAGE);
    }
}
