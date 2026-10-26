// ClavierSouris.java
package delaterrealassiette;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/* ________________________________________________________________
 * ACTION : Écoute le clavier et la souris et fournit, à chaque tic,
 *          l'état des 9 boutons du jeu sous forme de tableau de booléens.
 * [INFO] Les indices du tableau (BOUTON_...) sont les mêmes que ceux
 *        définis en haut de Joueur (toucheHaut = 0, ..., toucheSauter = 8),
 *        mais cette classe ne dépend pas de Joueur : on peut la tester seule.
 * [INFO] Un bouton vaut true s'il est enfoncé au moment du tic OU s'il a
 *        été appuyé entre les deux tics (un appui très court n'est pas perdu).
 * [INFO] Si plusieurs touches de chef sont actives, seule la dernière
 *        appuyée reste à true.
 * [DEPENDANCE] FenetreDeJeu l'ajoute comme écouteur et appelle
 *              getEtatBoutons() à chaque tic. Joueur reçoit le tableau
 *              dans miseAJourIntentions().
 * ________________________________________________________________ */
public class ClavierSouris implements KeyListener, MouseListener, MouseMotionListener {

    public static final int NB_BOUTONS = 9;

    // Indices des boutons dans le tableau (mêmes valeurs que dans Joueur)
    public static final int BOUTON_HAUT = 0;
    public static final int BOUTON_BAS = 1;
    public static final int BOUTON_GAUCHE = 2;
    public static final int BOUTON_DROITE = 3;
    public static final int BOUTON_PHILIPPE = 4;
    public static final int BOUTON_CYRIL = 5;
    public static final int BOUTON_NORBERT = 6;
    public static final int BOUTON_COMPETENCE = 7;
    public static final int BOUTON_SAUTER = 8;

    private final boolean[] enfonce = new boolean[NB_BOUTONS];
    private final boolean[] appuyeDepuisDernierTic = new boolean[NB_BOUTONS];

    // Indice (dans le tableau) de la dernière touche de chef appuyée, -1 si aucune
    private int dernierChef = -1;

    private int sourisX;
    private int sourisY;
    private boolean sourisEnfoncee;
    private boolean clicDepuisDernierTic;

    /* ________________________________________________________________
     * ACTION : Retourne l'indice du bouton associé à une touche, -1 sinon.
     * [INFO] Deux touches possibles par direction (ZQSD ou flèches).
     * ________________________________________________________________ */
    private int boutonDeLaTouche(int codeTouche) {
        switch (codeTouche) {
            case KeyEvent.VK_Z:
            case KeyEvent.VK_UP:
                return BOUTON_HAUT;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                return BOUTON_BAS;
            case KeyEvent.VK_Q:
            case KeyEvent.VK_LEFT:
                return BOUTON_GAUCHE;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                return BOUTON_DROITE;
            case KeyEvent.VK_1:
            case KeyEvent.VK_NUMPAD1:
                return BOUTON_PHILIPPE;
            case KeyEvent.VK_2:
            case KeyEvent.VK_NUMPAD2:
                return BOUTON_CYRIL;
            case KeyEvent.VK_3:
            case KeyEvent.VK_NUMPAD3:
                return BOUTON_NORBERT;
            case KeyEvent.VK_E:
                return BOUTON_COMPETENCE;
            case KeyEvent.VK_SPACE:
                return BOUTON_SAUTER;
            default:
                return -1;
        }
    }

    /* ________________________________________________________________
     * ACTION : Indique si un indice correspond à une touche de chef.
     * ________________________________________________________________ */
    private static boolean estToucheChef(int bouton) {
        return bouton == BOUTON_PHILIPPE
                || bouton == BOUTON_CYRIL
                || bouton == BOUTON_NORBERT;
    }

    /* ________________________________________________________________
     * ACTION : Retourne l'état des 9 boutons entre le tic précédent et
     *          maintenant, puis prépare le tic suivant.
     * [INFO] À appeler UNE seule fois par tic.
     * ________________________________________________________________ */
    public synchronized boolean[] getEtatBoutons() {
        boolean[] etat = new boolean[NB_BOUTONS];
        for (int i = 0; i < NB_BOUTONS; i++) {
            etat[i] = enfonce[i] || appuyeDepuisDernierTic[i];
            appuyeDepuisDernierTic[i] = false;
        }
        verifierDernierChef(etat);
        clicDepuisDernierTic = false;
        return etat;
    }

    /* ________________________________________________________________
     * ACTION : Ne laisse à true que la dernière touche de chef appuyée.
     * [INFO] Évite que Joueur reçoive deux changements de chef au même tic.
     * ________________________________________________________________ */
    private void verifierDernierChef(boolean[] etat) {
        boolean dernierChefActif = dernierChef != -1 && etat[dernierChef];

        etat[BOUTON_PHILIPPE] = false;
        etat[BOUTON_CYRIL] = false;
        etat[BOUTON_NORBERT] = false;

        if (dernierChefActif) {
            etat[dernierChef] = true;
        }
    }

    // ===================== Clavier =====================

    /* ________________________________________________________________
     * ACTION : Mémorise l'appui d'une touche du jeu.
     * ________________________________________________________________ */
    @Override
    public synchronized void keyPressed(KeyEvent evenement) {
        int bouton = boutonDeLaTouche(evenement.getKeyCode());
        if (bouton == -1) {
            return;
        }
        enfonce[bouton] = true;
        appuyeDepuisDernierTic[bouton] = true;
        if (estToucheChef(bouton)) {
            dernierChef = bouton;
        }
    }

    /* ________________________________________________________________
     * ACTION : Mémorise le relâchement d'une touche du jeu.
     * ________________________________________________________________ */
    @Override
    public synchronized void keyReleased(KeyEvent evenement) {
        int bouton = boutonDeLaTouche(evenement.getKeyCode());
        if (bouton != -1) {
            enfonce[bouton] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent evenement) {
        // Rien à faire ici
    }

    // ===================== Souris =====================

    @Override
    public synchronized void mousePressed(MouseEvent evenement) {
        sourisEnfoncee = true;
        clicDepuisDernierTic = true;
        sourisX = evenement.getX();
        sourisY = evenement.getY();
    }

    @Override
    public synchronized void mouseReleased(MouseEvent evenement) {
        sourisEnfoncee = false;
    }

    @Override
    public synchronized void mouseMoved(MouseEvent evenement) {
        sourisX = evenement.getX();
        sourisY = evenement.getY();
    }

    @Override
    public void mouseDragged(MouseEvent evenement) {
        mouseMoved(evenement);
    }

    @Override
    public void mouseClicked(MouseEvent evenement) {
        // Géré par mousePressed
    }

    @Override
    public void mouseEntered(MouseEvent evenement) {
        // Rien à faire ici
    }

    @Override
    public void mouseExited(MouseEvent evenement) {
        // Rien à faire ici
    }

    /* ________________________________________________________________
     * ACTION : Retourne la position de la souris dans la fenêtre (pixels).
     * ________________________________________________________________ */
    public synchronized int getSourisX() {
        return sourisX;
    }

    public synchronized int getSourisY() {
        return sourisY;
    }

    /* ________________________________________________________________
     * ACTION : Indique si le bouton de la souris est enfoncé ou a été
     *          cliqué depuis le dernier tic.
     * [INFO] À lire AVANT getEtatBoutons(), qui remet le clic à zéro.
     * ________________________________________________________________ */
    public synchronized boolean isClicSouris() {
        return sourisEnfoncee || clicDepuisDernierTic;
    }
}
