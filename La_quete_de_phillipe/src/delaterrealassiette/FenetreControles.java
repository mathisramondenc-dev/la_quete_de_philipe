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

/**
 * Écran « Contrôles » : liste des touches du jeu et de leur effet, ouvert
 * depuis le menu (Retour ou Échap pour y revenir).
 *
 * Le contenu est décrit par des tableaux de données {touches, action,
 * détail} regroupés par section ; ajouterSection() les met en page en deux
 * colonnes. Modifier l'écran revient donc à modifier ces tableaux.
 * Attention : ce texte n'est pas lié au code des touches (ClavierSouris,
 * F11 dans FenetreDeJeu) ; il faut le mettre à jour à la main si elles
 * changent.
 */
public class FenetreControles extends Fenetre {

    private static final Color COULEUR_SECTION = new Color(255, 220, 95);
    private static final Color COULEUR_DETAIL = new Color(200, 195, 185);

    // Une ligne = {touches séparées par " ou ", action, détail facultatif}
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

    /** Construit l'écran : colonne gauche (déplacements, autres), colonne droite (chefs, compétence). */
    public FenetreControles() {
        super();

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

    /**
     * Ajoute à la colonne un titre de section suivi d'une ligne par entrée
     * (touches à gauche, action et détail à droite).
     *
     * @param ligne première ligne libre dans la grille de la colonne
     * @return première ligne libre après la section
     */
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

    /** "Q ou ←" devient deux touches dessinées [Q] ou [←]. */
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

    /** Action en gras, détail en plus petit en dessous (mise en forme HTML du JLabel). */
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

    /** Associe la touche Échap au retour au menu. */
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

    private void retourMenu() {
        new FenetreMenu().setVisible(true);
        dispose();
    }

    /**
     * Étiquette dessinée comme une touche de clavier : dessus clair, dessous
     * plus foncé pour l'effet de relief, texte centré sur le dessus.
     */
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
            g.setColor(new Color(150, 145, 140));
            g.fillRoundRect(0, relief, getWidth(), getHeight() - relief, arrondi, arrondi);
            g.setColor(new Color(240, 236, 228));
            g.fillRoundRect(0, 0, getWidth(), getHeight() - relief, arrondi, arrondi);
            g.dispose();
            Graphics2D gTexte = (Graphics2D) graphismes.create();
            gTexte.translate(0, -relief / 2);
            super.paintComponent(gTexte);
            gTexte.dispose();
        }
    }
}
