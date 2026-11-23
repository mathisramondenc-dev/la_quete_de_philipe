package delaterrealassiette;

import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

/** Création d'une salle BDD partagée et lancement du premier client. */
public class FenetreHeberger extends Fenetre {
    private final JTextField pseudo=new JTextField(15), code=new JTextField(15);
    private final JPasswordField mdp=new JPasswordField(15);
    public FenetreHeberger() {
        super(); JPanel fond=creerPanneauFond(false); fond.setBackground(COULEUR_FOND); fond.setBorder(BorderFactory.createEmptyBorder(20,30,20,30));
        JLabel titre=creerTexte("Héberger une partie",30,true); titre.setHorizontalAlignment(JLabel.CENTER); fond.add(titre,BorderLayout.NORTH);
        JPanel form=new JPanel(new GridBagLayout()); form.setOpaque(false);
        ajouterLigneFormulaire(form,0,"Pseudo :",pseudo); ajouterLigneFormulaire(form,1,"Code à partager :",code); ajouterLigneFormulaire(form,2,"Mot de passe :",mdp); fond.add(form,BorderLayout.CENTER);
        JButton back=creerBouton("Retour",120,45,16,e->retour()); JButton go=creerBouton("Créer",160,45,18,e->creer());
        JPanel buttons=new JPanel(new FlowLayout());buttons.setOpaque(false);buttons.add(back);buttons.add(go);fond.add(buttons,BorderLayout.SOUTH);
        setContentPane(fond);getRootPane().setDefaultButton(go);setMinimumSize(new Dimension(520,0));finaliserFenetre();
    }
    private void creer() {
        String p=pseudo.getText().trim(), c=code.getText().trim(), m=new String(mdp.getPassword());
        String erreur=InfosPartie.verifierPseudo(p); if(erreur==null) erreur=InfosPartie.verifierIdentifiant(c); if(erreur==null) erreur=InfosPartie.verifierMotDePasse(m);
        if(erreur!=null){JOptionPane.showMessageDialog(this,erreur,"Saisie incorrecte",JOptionPane.ERROR_MESSAGE);return;}
        try { lancer(GestionBDD.heberger(p,c,m)); } catch(SQLException e){JOptionPane.showMessageDialog(this,"Création impossible (code déjà utilisé ou BDD inaccessible): "+e.getMessage(),"Erreur BDD",JOptionPane.ERROR_MESSAGE);}
    }
    private void lancer(InfosPartie i){FenetreDeJeu f=new FenetreDeJeu(new Jeu("niveau1.txt"),i);f.setVisible(true);f.demarrer();dispose();}
    private void retour(){new FenetreMenu().setVisible(true);dispose();}
}
