package delaterrealassiette;

import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

/**
 * Écran « Héberger une partie » : le joueur choisit son pseudo, un code de
 * partie à communiquer aux autres et un mot de passe.
 *
 * Après validation des champs (règles d'InfosPartie), la partie est créée
 * dans la base par GestionBDD.heberger(), puis la fenêtre de jeu s'ouvre au
 * niveau 1. Si le code est déjà pris ou la base inaccessible, un message
 * d'erreur s'affiche et on reste sur le formulaire.
 */
public class FenetreHeberger extends Fenetre {
    private final JTextField pseudo=new JTextField(15), code=new JTextField(15);
    private final JPasswordField mdp=new JPasswordField(15);

    /** Construit le formulaire plein écran (3 champs, boutons Retour et Créer ; Entrée = Créer). */
    public FenetreHeberger() {
        super();
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        ajouterLigneFormulaire(form, 0, "Pseudo :", pseudo);
        ajouterLigneFormulaire(form, 1, "Code à partager :", code);
        ajouterLigneFormulaire(form, 2, "Mot de passe :", mdp);

        JButton back = creerBouton("Retour", t(170), t(56), t(22), e -> retour());
        JButton go = creerBouton("Créer", t(220), t(56), t(24), e -> creer());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, t(20), 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(t(20), 0, 0, 0));
        buttons.add(back);
        buttons.add(go);

        JPanel contenu = new JPanel(new BorderLayout());
        contenu.setOpaque(false);
        contenu.add(form, BorderLayout.CENTER);
        contenu.add(buttons, BorderLayout.SOUTH);

        setContentPane(creerEcranPleinEcran("Héberger une partie", contenu));
        getRootPane().setDefaultButton(go);
        finaliserFenetre();
        passerEnPleinEcran();
    }
    /** Vérifie les champs (premier message d'erreur affiché), crée la partie en base puis la lance. */
    private void creer() {
        String p=pseudo.getText().trim(), c=code.getText().trim(), m=new String(mdp.getPassword());
        String erreur=InfosPartie.verifierPseudo(p); if(erreur==null) erreur=InfosPartie.verifierIdentifiant(c); if(erreur==null) erreur=InfosPartie.verifierMotDePasse(m);
        if(erreur!=null){JOptionPane.showMessageDialog(this,erreur,"Saisie incorrecte",JOptionPane.ERROR_MESSAGE);return;}
        try { lancer(GestionBDD.heberger(p,c,m)); } catch(SQLException e){JOptionPane.showMessageDialog(this,"Création impossible (code déjà utilisé ou BDD inaccessible): "+e.getMessage(),"Erreur BDD",JOptionPane.ERROR_MESSAGE);}
    }
    /** Ouvre la fenêtre de jeu multijoueur au niveau 1 et ferme le formulaire. */
    private void lancer(InfosPartie i){FenetreDeJeu f=new FenetreDeJeu(new Jeu("niveau1.txt"),i);f.setVisible(true);f.demarrer();dispose();}
    private void retour(){new FenetreMenu().setVisible(true);dispose();}
}
