package jeu.ui;
import javax.swing.*; import java.awt.*;
/** Menu principal. */
public class FenetreMenu extends JFrame { public FenetreMenu(){super("De La Terre à l'assiette");setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);JButton debut=new JButton("Début de partie");debut.addActionListener(e->{dispose();new FenetreDeJeu().setVisible(true);});add(debut,BorderLayout.CENTER);setSize(360,150);setLocationRelativeTo(null);}
 public static void main(String[] args){SwingUtilities.invokeLater(()->new FenetreMenu().setVisible(true));} }
