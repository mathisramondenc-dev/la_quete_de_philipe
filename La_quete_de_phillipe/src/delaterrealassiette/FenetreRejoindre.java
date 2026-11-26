package delaterrealassiette;

import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

/** Écran de connexion à un code de partie existant dans MariaDB. */
public class FenetreRejoindre extends Fenetre {
    private final JTextField pseudo=new JTextField(15), code=new JTextField(15);
    private final JPasswordField mdp=new JPasswordField(15);
    /* ________________________________________________________________
     * ACTION : Construit l'écran "Rejoindre une partie" en plein écran
     *          (fond avec les 4 chefs, formulaire au centre).
     * ________________________________________________________________ */
    public FenetreRejoindre() {
        super();
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        ajouterLigneFormulaire(form, 0, "Pseudo :", pseudo);
        ajouterLigneFormulaire(form, 1, "Code de partie :", code);
        ajouterLigneFormulaire(form, 2, "Mot de passe :", mdp);

        JButton back = creerBouton("Retour", t(170), t(56), t(22), e -> retour());
        JButton liste = creerBouton("Joueurs en ligne", t(240), t(56), t(20), e -> lister());
        JButton go = creerBouton("Rejoindre", t(220), t(56), t(24), e -> rejoindre());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, t(20), 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(t(20), 0, 0, 0));
        buttons.add(back);
        buttons.add(liste);
        buttons.add(go);

        JPanel contenu = new JPanel(new BorderLayout());
        contenu.setOpaque(false);
        contenu.add(form, BorderLayout.CENTER);
        contenu.add(buttons, BorderLayout.SOUTH);

        setContentPane(creerEcranPleinEcran("Rejoindre une partie", contenu));
        getRootPane().setDefaultButton(go);
        finaliserFenetre();
        passerEnPleinEcran();
    }
    private void rejoindre(){
        String p=pseudo.getText().trim(),c=code.getText().trim(),m=new String(mdp.getPassword());String err=InfosPartie.verifierPseudo(p);if(err==null)err=InfosPartie.verifierIdentifiant(c);if(err==null)err=InfosPartie.verifierMotDePasse(m);
        if(err!=null){JOptionPane.showMessageDialog(this,err,"Saisie incorrecte",JOptionPane.ERROR_MESSAGE);return;}
        try{InfosPartie i=GestionBDD.rejoindre(p,c,m);FenetreDeJeu f=new FenetreDeJeu(new Jeu("niveau1.txt"),i);f.setVisible(true);f.demarrer();dispose();}
        catch(SQLException e){JOptionPane.showMessageDialog(this,e.getMessage(),"Connexion impossible",JOptionPane.ERROR_MESSAGE);}
    }
    private void lister(){String c=code.getText().trim();String err=InfosPartie.verifierIdentifiant(c);if(err!=null){JOptionPane.showMessageDialog(this,err,"Code requis",JOptionPane.ERROR_MESSAGE);return;}
        try{java.util.List<String> js=GestionBDD.listerJoueurs(c);JOptionPane.showMessageDialog(this,js.isEmpty()?"Aucun joueur en ligne.":String.join("\n",js),"Joueurs de "+c,JOptionPane.INFORMATION_MESSAGE);}
        catch(SQLException e){JOptionPane.showMessageDialog(this,"Lecture impossible: "+e.getMessage(),"Erreur BDD",JOptionPane.ERROR_MESSAGE);}
    }
    private void retour(){new FenetreMenu().setVisible(true);dispose();}
}
