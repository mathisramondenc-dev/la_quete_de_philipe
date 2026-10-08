package jeu.ui;
import javax.swing.*; import java.awt.*; import jeu.core.Jeu; import jeu.entites.Avatar; import jeu.entites.monstres.MonstreAleatoire; import jeu.map.Carte;
/** Fenetre de jeu et panneau anime a environ 60 images/seconde. */
public class FenetreDeJeu extends JFrame { public FenetreDeJeu(){super("De La Terre à l'assiette - Partie");setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);PanneauJeu panneau=new PanneauJeu();setContentPane(panneau);pack();setLocationRelativeTo(null);panneau.requestFocusInWindow();}
 public static class PanneauJeu extends JPanel {private final Jeu jeu=new Jeu();private final Carte carte=new Carte(800,600);private final Timer timer;
  public PanneauJeu(){setPreferredSize(new Dimension(800,600));setFocusable(true);Avatar a=new Avatar(1,100,100,true);jeu.ajouterAvatar(a);jeu.ajouterMonstre(new MonstreAleatoire(1,400,300));addKeyListener(a);timer=new Timer(16,e->{jeu.miseAJour();repaint();});timer.start();}
  protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g;carte.rendu(g2);for(Avatar a:jeu.getAvatars())a.rendu(g2);for(jeu.entites.monstres.Monstre m:jeu.getMonstres())m.rendu(g2);}
 }
}
