// FenetreHeberger.java
package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/* ________________________________________________________________
 * ACTION : Fenêtre pour héberger (créer) une partie multijoueur.
 *          L'hôte choisit son pseudo, un mot de passe facultatif,
 *          le nombre de joueurs et le niveau. Un identifiant de partie
 *          est généré : l'hôte le donne aux autres joueurs.
 * [INFO] Les champs sont vérifiés avant de lancer la partie, avec un
 *        message d'erreur (JOptionPane) si quelque chose ne va pas.
 * [DEPENDANCE] Ouverte par FenetreMenu, ouvre FenetreDeJeu.
 *              Plus tard : enregistrer la partie dans la BDD (voir TODO).
 * ________________________________________________________________ */
public class FenetreHeberger extends Fenetre {

    private static final String[] NOMS_NIVEAUX = {"Niveau 1", "Niveau 2"};
    private static final String[] FICHIERS_NIVEAUX = {"niveau1.txt", "niveau2.txt"};

    private final JTextField champPseudo;
    private final JPasswordField champMotDePasse;
    private final JSpinner choixNbJoueurs;
    private final JComboBox<String> choixNiveau;
    private final JLabel texteIdentifiant;
    private String identifiantPartie;

    /* ________________________________________________________________
     * ACTION : Construit le formulaire d'hébergement.
     * ________________________________________________________________ */
    public FenetreHeberger() {
        super();

        JPanel fond = new JPanel(new BorderLayout());
        fond.setBackground(COULEUR_FOND);
        fond.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titre = creerTexte("Héberger une partie", 30, true);
        titre.setHorizontalAlignment(JLabel.CENTER);
        fond.add(titre, BorderLayout.NORTH);

        // ----- Formulaire -----
        champPseudo = new JTextField(15);
        champMotDePasse = new JPasswordField(15);
        choixNbJoueurs = new JSpinner(new SpinnerNumberModel(
                InfosPartie.NB_JOUEURS_MAX, InfosPartie.NB_JOUEURS_MIN,
                InfosPartie.NB_JOUEURS_MAX, 1));
        choixNiveau = new JComboBox<>(NOMS_NIVEAUX);

        identifiantPartie = InfosPartie.genererIdentifiant();
        texteIdentifiant = creerTexte(identifiantPartie, 22, true);
        JButton boutonNouvelId = creerBouton("Nouveau", 100, 28, 12,
                e -> changerIdentifiant());
        JPanel ligneIdentifiant = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        ligneIdentifiant.setOpaque(false);
        ligneIdentifiant.add(texteIdentifiant);
        ligneIdentifiant.add(boutonNouvelId);

        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setOpaque(false);
        ajouterLigneFormulaire(formulaire, 0, "Pseudo :", champPseudo);
        ajouterLigneFormulaire(formulaire, 1, "Mot de passe (facultatif) :", champMotDePasse);
        ajouterLigneFormulaire(formulaire, 2, "Nombre de joueurs max :", choixNbJoueurs);
        ajouterLigneFormulaire(formulaire, 3, "Niveau :", choixNiveau);
        ajouterLigneFormulaire(formulaire, 4, "Identifiant de la partie :", ligneIdentifiant);

        JLabel aide = creerTexte("Donne l'identifiant (et le mot de passe) "
                + "aux joueurs qui veulent te rejoindre.", 13, false);
        aide.setFont(aide.getFont().deriveFont(Font.ITALIC));
        JPanel centre = new JPanel(new BorderLayout());
        centre.setOpaque(false);
        centre.add(formulaire, BorderLayout.CENTER);
        centre.add(aide, BorderLayout.SOUTH);
        fond.add(centre, BorderLayout.CENTER);

        // ----- Boutons -----
        JButton boutonCreer = creerBouton("Créer la partie", 200, 45, 18, e -> creerPartie());
        JButton boutonRetour = creerBouton("Retour", 120, 45, 16, e -> retourMenu());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        boutons.setOpaque(false);
        boutons.add(boutonRetour);
        boutons.add(boutonCreer);
        fond.add(boutons, BorderLayout.SOUTH);

        setContentPane(fond);
        getRootPane().setDefaultButton(boutonCreer); // Entrée = Créer la partie
        setMinimumSize(new Dimension(560, 0));
        finaliserFenetre();
    }

    /* ________________________________________________________________
     * ACTION : Génère un nouvel identifiant de partie.
     * ________________________________________________________________ */
    private void changerIdentifiant() {
        identifiantPartie = InfosPartie.genererIdentifiant();
        texteIdentifiant.setText(identifiantPartie);
    }

    /* ________________________________________________________________
     * ACTION : Vérifie les champs, puis crée la partie et ouvre le jeu.
     * [INFO] En cas d'erreur, un message s'affiche et on reste ici.
     * ________________________________________________________________ */
    private void creerPartie() {
        String pseudo = champPseudo.getText().trim();
        String erreur = InfosPartie.verifierPseudo(pseudo);
        if (erreur != null) {
            afficherErreur(erreur);
            champPseudo.requestFocus();
            return;
        }

        String motDePasse = new String(champMotDePasse.getPassword());
        int nbJoueurs = (Integer) choixNbJoueurs.getValue();
        String fichierNiveau = FICHIERS_NIVEAUX[choixNiveau.getSelectedIndex()];

        InfosPartie infos = InfosPartie.pourHote(pseudo, identifiantPartie,
                motDePasse, nbJoueurs, fichierNiveau);

        // TODO BDD : enregistrer la partie (identifiant, mot de passe,
        //            nombre de joueurs, niveau, pseudo de l'hôte) et
        //            vérifier que l'identifiant n'est pas déjà utilisé.
        System.out.println("Partie créée : " + infos);

        lancerPartie(infos);
    }

    /* ________________________________________________________________
     * ACTION : Ouvre la fenêtre de jeu et ferme celle-ci.
     * ________________________________________________________________ */
    private void lancerPartie(InfosPartie infos) {
        FenetreDeJeu fenetreDeJeu = new FenetreDeJeu(new Jeu(infos.getFichierNiveau()), infos);
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
        JOptionPane.showMessageDialog(this, message, "Impossible de créer la partie",
                JOptionPane.ERROR_MESSAGE);
    }
}
