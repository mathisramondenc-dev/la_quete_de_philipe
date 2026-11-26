// FenetreControles.java
package delaterrealassiette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/* ________________________________________________________________
 * ACTION : Écran "Contrôles" : affiche toutes les touches du jeu et
 *          l'action de chacune (déplacements, chefs, compétence...).
 * [INFO] S'ouvre en plein écran depuis le menu. Retour ou Échap pour
 *        revenir au menu.
 * [INFO] Les touches affichées sont celles de ClavierSouris (et F11 de
 *        FenetreDeJeu) : penser à mettre ce texte à jour si elles changent.
 * [DEPENDANCE] Ouverte par FenetreMenu.
 * ________________________________________________________________ */
public class FenetreControles extends Fenetre {

    private static final Color COULEUR_SECTION = new Color(255, 220, 95);
    private static final Color COULEUR_DETAIL = new Color(200, 195, 185);

    // Lignes de l'écran : touches (séparées par " ou ") | action | détail
    private static final String[][] DEPLACEMENTS = {
        {"Q ou ←", "Aller à gauche", ""},
        {"D ou →", "Aller à droite", ""},
        {"Espace", "Sauter", ""},
        {"Z ou ↑", "Monter à l'échelle", ""},
        {"S ou ↓", "Descendre de l'échelle", ""}
    };
    private static final String[][] AUTRES = {
        {"F11", "Plein écran", "pendant la partie"},
        {"Espace", "Passer l'écran de transition", "entre deux niveaux"},
        {"Entrée", "Valider", "Héberger / Rejoindre"},
        {"Échap", "Revenir au menu", "depuis cet écran"}
    };
    private static final String[][] CHEFS = {
        {"1", "Philippe", "pousse les caisses"},
        {"2", "Cyril", "le plus rapide, saute le plus haut"},
        {"3", "Norbert", "petit : se faufile partout"},
        {"4", "Maïté", "soigne l'équipe avec E"}
    };
    private static final String[][] COMPETENCE = {
        {"E", "Norbert : active un levier", "quand il est juste à côté"},
        {"E", "Maïté : soigne", "elle et les joueurs proches, toutes les 15 s"}
    };

    /* ________________________________________________________________
     * ACTION : Construit l'écran des contrôles.
     * ________________________________________________________________ */
    public FenetreControles() {
        super();

        // Deux colonnes : déplacements + autres | chefs + compétence
        JPanel colonnes = new JPanel(new GridBagLayout());
        colonnes.setOpaque(false);
        GridBagConstraints placement = new GridBagConstraints();
        placement.anchor = GridBagConstraints.NORTHWEST;
        placement.insets = new Insets(0, 0, 0, t(30));

        JPanel gauche = creerColonne();
        int ligne = ajouterSection(gauche, 0, "Se déplacer", DEPLACEMENTS);
        ajouterSection(gauche, ligne, "Autres touches", AUTRES);
        colonnes.add(gauche, placement);

        JPanel droite = creerColonne();
        ligne = ajouterSection(droite, 0, "Changer de chef (aussi pavé numérique)", CHEFS);
        ajouterSection(droite, ligne, "Compétence", COMPETENCE);
        placement.gridx = 1;
        placement.insets = new Insets(0, t(30), 0, 0);
        colonnes.add(droite, placement);

        // Bouton Retour
        JButton boutonRetour = creerBouton("Retour", t(220), t(56), t(22), e -> retourMenu());
        JPanel bas = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bas.setOpaque(false);
        bas.setBorder(BorderFactory.createEmptyBorder(t(25), 0, 0, 0));
        bas.add(boutonRetour);

        JPanel contenu = new JPanel(new BorderLayout());
        contenu.setOpaque(false);
        contenu.add(colonnes, BorderLayout.CENTER);
        contenu.add(bas, BorderLayout.SOUTH);

        setContentPane(creerEcranPleinEcran("Contrôles", contenu));
        getRootPane().setDefaultButton(boutonRetour);
        fermerAvecEchap();
        finaliserFenetre();
        passerEnPleinEcran();
    }

    private JPanel creerColonne() {
        JPanel colonne = new JPanel(new GridBagLayout());
        colonne.setOpaque(false);
        return colonne;
    }

    /* ________________________________________________________________
     * ACTION : Ajoute un titre de section puis ses lignes dans une colonne.
     * [INFO] Retourne le numéro de la ligne suivante.
     * ________________________________________________________________ */
    private int ajouterSection(JPanel colonne, int ligne, String titre, String[][] lignes) {
        GridBagConstraints placement = new GridBagConstraints();
        placement.gridx = 0;
        placement.gridy = ligne++;
        placement.gridwidth = 2;
        placement.anchor = GridBagConstraints.WEST;
        placement.insets = new Insets(ligne == 1 ? 0 : t(22), 0, t(8), 0);
        JLabel texteTitre = creerTexte(titre, t(24), true);
        texteTitre.setForeground(COULEUR_SECTION);
        colonne.add(texteTitre, placement);

        for (String[] l : lignes) {
            placement = new GridBagConstraints();
            placement.gridy = ligne++;
            placement.insets = new Insets(t(5), 0, t(5), t(18));
            placement.anchor = GridBagConstraints.WEST;

            placement.gridx = 0;
            colonne.add(creerTouches(l[0]), placement);

            placement.gridx = 1;
            placement.insets = new Insets(t(5), 0, t(5), 0);
            colonne.add(creerAction(l[1], l[2]), placement);
        }
        return ligne;
    }

    /* ________________________________________________________________
     * ACTION : Crée les touches d'une ligne ("Q ou ←" -> [Q] ou [←]).
     * ________________________________________________________________ */
    private JPanel creerTouches(String texte) {
        JPanel touches = new JPanel(new FlowLayout(FlowLayout.LEFT, t(6), 0));
        touches.setOpaque(false);
        String[] parties = texte.split(" ou ");
        for (int i = 0; i < parties.length; i++) {
            if (i > 0) {
                touches.add(creerTexte("ou", t(18), false));
            }
            touches.add(new Touche(parties[i]));
        }
        touches.setPreferredSize(new Dimension(t(165), t(46)));
        return touches;
    }

    /* ________________________________________________________________
     * ACTION : Crée le texte d'une action, avec son détail en plus petit.
     * ________________________________________________________________ */
    private JLabel creerAction(String action, String detail) {
        String html = "<html><b>" + action + "</b>"
                + (detail.isEmpty() ? "" : "<br><span style='font-size:" + t(15)
                + "px'>" + detail + "</span>") + "</html>";
        JLabel texte = creerTexte(html, t(20), false);
        texte.setForeground(COULEUR_TEXTE);
        if (!detail.isEmpty()) {
            texte.setToolTipText(detail);
        }
        return texte;
    }

    /* ________________________________________________________________
     * ACTION : La touche Échap ramène au menu.
     * ________________________________________________________________ */
    private void fermerAvecEchap() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "retourMenu");
        getRootPane().getActionMap().put("retourMenu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evenement) {
                retourMenu();
            }
        });
    }

    /* ________________________________________________________________
     * ACTION : Revient au menu principal.
     * ________________________________________________________________ */
    private void retourMenu() {
        new FenetreMenu().setVisible(true);
        dispose();
    }

    /* ________________________________________________________________
     * ACTION : Dessine une touche de clavier (carré clair arrondi avec
     *          le nom de la touche).
     * ________________________________________________________________ */
    private class Touche extends JLabel {

        Touche(String nom) {
            super(nom, JLabel.CENTER);
            setFont(new Font("SansSerif", Font.BOLD, t(nom.length() > 2 ? 17 : 20)));
            setForeground(new Color(35, 30, 30));
            int largeur = Math.max(t(42), getFontMetrics(getFont()).stringWidth(nom) + t(24));
            setPreferredSize(new Dimension(largeur, t(42)));
        }

        @Override
        protected void paintComponent(Graphics graphismes) {
            Graphics2D g = (Graphics2D) graphismes.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arrondi = t(10);
            int relief = t(4);
            g.setColor(new Color(150, 145, 140));          // dessous de la touche
            g.fillRoundRect(0, relief, getWidth(), getHeight() - relief, arrondi, arrondi);
            g.setColor(new Color(240, 236, 228));          // dessus de la touche
            g.fillRoundRect(0, 0, getWidth(), getHeight() - relief, arrondi, arrondi);
            g.dispose();
            // Le texte, un peu remonté pour être centré sur le dessus
            Graphics2D gTexte = (Graphics2D) graphismes.create();
            gTexte.translate(0, -relief / 2);
            super.paintComponent(gTexte);
            gTexte.dispose();
        }
    }
}
