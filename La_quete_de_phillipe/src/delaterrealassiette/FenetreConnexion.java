package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Écran « Se connecter à une partie » : pseudo, identifiant de la partie et
 * mot de passe, puis entrée dans une partie existante via la base.
 *
 * Remarque : le menu ouvre désormais FenetreRejoindre, qui remplit le même
 * rôle ; cette fenêtre n'est plus ouverte nulle part.
 *
 * Validation : les champs sont vérifiés dans l'ordre avec les règles
 * d'InfosPartie ; au premier champ invalide, un message s'affiche et le
 * curseur est placé dans ce champ.
 */
public class FenetreConnexion extends Fenetre {

    /** Niveau de départ, tant que la base ne fournit pas celui de la partie. */
    private static final String NIVEAU_PAR_DEFAUT = "niveau1.txt";

    private final JTextField champPseudo;
    private final JTextField champIdentifiant;
    private final JPasswordField champMotDePasse;

    /** Construit le formulaire plein écran (Entrée = Se connecter). */
    public FenetreConnexion() {
        super();

        champPseudo = new JTextField(15);
        champIdentifiant = new JTextField(15);
        champMotDePasse = new JPasswordField(15);

        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setOpaque(false);
        ajouterLigneFormulaire(formulaire, 0, "Pseudo :", champPseudo);
        ajouterLigneFormulaire(formulaire, 1, "Identifiant de la partie :", champIdentifiant);
        ajouterLigneFormulaire(formulaire, 2, "Mot de passe :", champMotDePasse);

        JButton boutonConnexion = creerBouton("Se connecter", t(240), t(56), t(24), e -> seConnecter());
        JButton boutonRetour = creerBouton("Retour", t(170), t(56), t(22), e -> retourMenu());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.CENTER, t(20), 0));
        boutons.setOpaque(false);
        boutons.setBorder(BorderFactory.createEmptyBorder(t(20), 0, 0, 0));
        boutons.add(boutonRetour);
        boutons.add(boutonConnexion);

        JPanel contenu = new JPanel(new BorderLayout());
        contenu.setOpaque(false);
        contenu.add(formulaire, BorderLayout.CENTER);
        contenu.add(boutons, BorderLayout.SOUTH);

        setContentPane(creerEcranPleinEcran("Se connecter à une partie", contenu));
        getRootPane().setDefaultButton(boutonConnexion);
        finaliserFenetre();
        passerEnPleinEcran();
    }

    /**
     * Vérifie les trois champs puis rejoint la partie en base. La session
     * obtenue est indispensable pour que la synchronisation publie les actions.
     */
    private void seConnecter() {
        String pseudo = champPseudo.getText().trim();
        String identifiant = champIdentifiant.getText().trim();
        String motDePasse = new String(champMotDePasse.getPassword());

        if (!champValide(InfosPartie.verifierPseudo(pseudo), champPseudo)
                || !champValide(InfosPartie.verifierIdentifiant(identifiant), champIdentifiant)
                || !champValide(InfosPartie.verifierMotDePasse(motDePasse), champMotDePasse)) {
            return;
        }

        try {
            lancerPartie(GestionBDD.rejoindre(pseudo, identifiant, motDePasse));
        } catch (SQLException e) {
            afficherErreur("Connexion impossible : " + e.getMessage());
        }
    }

    /**
     * @param erreur résultat d'une méthode InfosPartie.verifier...()
     * @return true si erreur vaut null ; sinon affiche l'erreur, place le
     *         curseur dans le champ et renvoie false
     */
    private boolean champValide(String erreur, JTextField champ) {
        if (erreur == null) {
            return true;
        }
        afficherErreur(erreur);
        champ.requestFocus();
        return false;
    }

    private void lancerPartie(InfosPartie infos) {
        FenetreDeJeu fenetreDeJeu = new FenetreDeJeu(new Jeu(NIVEAU_PAR_DEFAUT), infos);
        fenetreDeJeu.setVisible(true);
        fenetreDeJeu.demarrer();
        dispose();
    }

    private void retourMenu() {
        new FenetreMenu().setVisible(true);
        dispose();
    }

    private void afficherErreur(String message) {
        JOptionPane.showMessageDialog(this, message, "Connexion impossible",
                JOptionPane.ERROR_MESSAGE);
    }
}
