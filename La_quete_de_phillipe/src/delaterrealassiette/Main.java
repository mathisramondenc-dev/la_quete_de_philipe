package delaterrealassiette;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Ouvre le menu d'accueil (bouton START -> FenetreDeJeu)
        SwingUtilities.invokeLater(() -> new FenetreMenu().setVisible(true));
    }
}