package delaterrealassiette;

import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

/** Écran de connexion à un code de partie existant dans MariaDB. */
public class FenetreRejoindre extends Fenetre {
    private final JTextField pseudo=new JTextField(15), code=new JTextField(15);
    private final JPasswordField mdp=new JPasswordField(15);
    public FenetreRejoindre() {
        super(); JPanel fond=creerPanneauFond(false);fond.setBackground(COULEUR_FOND);fond.setBorder(BorderFactory.createEmptyBorder(20,30,20,30));
        JLabel titre=creerTexte("Rejoindre une partie",30,true);titre.setHorizontalAlignment(JLabel.CENTER);fond.add(titre,BorderLayout.NORTH);
        JPanel form=new JPanel(new GridBagLayout());form.setOpaque(false);ajouterLigneFormulaire(form,0,"Pseudo :",pseudo);ajouterLigneFormulaire(form,1,"Code de partie :",code);ajouterLigneFormulaire(form,2,"Mot de passe :",mdp);fond.add(form,BorderLayout.CENTER);
        JButton back=creerBouton("Retour",120,45,16,e->retour()); JButton go=creerBouton("Rejoindre",170,45,18,e->rejoindre());
        JPanel buttons=new JPanel(new FlowLayout());buttons.setOpaque(false);buttons.add(back);buttons.add(creerBouton("Joueurs en ligne",170,45,14,e->lister()));buttons.add(go);fond.add(buttons,BorderLayout.SOUTH);
        setContentPane(fond);getRootPane().setDefaultButton(go);setMinimumSize(new Dimension(560,0));finaliserFenetre();
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
