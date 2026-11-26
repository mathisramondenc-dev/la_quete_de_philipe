package delaterrealassiette;

import javax.swing.SwingUtilities;

/**
 * Point d'entrée de l'application.
 *
 * Ouvre le menu d'accueil sur le thread graphique de Swing (EDT), comme
 * l'exige Swing pour toute création de fenêtre.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FenetreMenu().setVisible(true));
    }
}
