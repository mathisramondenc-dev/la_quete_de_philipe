// Fenetre.java
package delaterrealassiette;

import javax.swing.JFrame;

/* ________________________________________________________________
 * ACTION : Base commune des fenêtres du jeu (titre, taille, fermeture).
 * [DEPENDANCE] FenetreMenu et FenetreDeJeu en héritent.
 * ________________________________________________________________ */
public abstract class Fenetre extends JFrame {

    public static final String TITRE = "La Quête de Philippe";
    public static final int LARGEUR = 800;
    public static final int HAUTEUR = 320;

    /* ________________________________________________________________
     * ACTION : Configure la fenêtre sans l'afficher.
     * [INFO] Appeler setVisible(true) après la construction.
     * ________________________________________________________________ */
    public Fenetre() {
        super(TITRE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
    }

    /* ________________________________________________________________
     * ACTION : Ajuste la taille au contenu et centre la fenêtre à l'écran.
     * [INFO] À appeler à la fin du constructeur des classes filles.
     * ________________________________________________________________ */
    protected void finaliserFenetre() {
        pack();
        setLocationRelativeTo(null);
    }
}
