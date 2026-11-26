package delaterrealassiette;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Écoute le clavier et la souris et fournit, une fois par tick, l'état des
 * 10 « boutons » du jeu sous forme d'un tableau de booléens.
 *
 * Pourquoi un tableau plutôt que des événements : Swing envoie les touches
 * à n'importe quel moment, alors que le jeu avance par ticks. On mémorise
 * donc les événements au fil de l'eau, puis getEtatBoutons() en fait une
 * photo à chaque tick. Un bouton vaut true s'il est enfoncé au moment du
 * tick OU s'il a été appuyé depuis le tick précédent : un appui très bref
 * n'est jamais perdu.
 *
 * Les indices BOUTON_* sont identiques aux constantes touche* de Joueur,
 * mais cette classe ne dépend pas de Joueur et peut être testée seule
 * (écran de debug). Plusieurs touches sont acceptées par action : ZQSD ou
 * flèches, chiffres du clavier ou du pavé numérique.
 */
public class ClavierSouris implements KeyListener, MouseListener, MouseMotionListener {

    public static final int NB_BOUTONS = 10;

    public static final int BOUTON_HAUT = 0;
    public static final int BOUTON_BAS = 1;
    public static final int BOUTON_GAUCHE = 2;
    public static final int BOUTON_DROITE = 3;
    public static final int BOUTON_PHILIPPE = 4;
    public static final int BOUTON_CYRIL = 5;
    public static final int BOUTON_NORBERT = 6;
    public static final int BOUTON_COMPETENCE = 7;
    public static final int BOUTON_SAUTER = 8;
    public static final int BOUTON_CHEF4 = 9;

    private final boolean[] enfonce = new boolean[NB_BOUTONS];
    private final boolean[] appuyeDepuisDernierTic = new boolean[NB_BOUTONS];

    /** Dernière touche de chef appuyée (indice BOUTON_*), -1 si aucune. */
    private int dernierChef = -1;

    private int sourisX;
    private int sourisY;
    private boolean sourisEnfoncee;
    private boolean clicDepuisDernierTic;

    /** @return indice BOUTON_* de la touche, ou -1 si elle n'est pas utilisée par le jeu */
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
            case KeyEvent.VK_4:
            case KeyEvent.VK_NUMPAD4:
                return BOUTON_CHEF4;
            case KeyEvent.VK_E:
                return BOUTON_COMPETENCE;
            case KeyEvent.VK_SPACE:
                return BOUTON_SAUTER;
            default:
                return -1;
        }
    }

    private static boolean estToucheChef(int bouton) {
        return bouton == BOUTON_PHILIPPE
                || bouton == BOUTON_CYRIL
                || bouton == BOUTON_NORBERT
                || bouton == BOUTON_CHEF4;
    }

    /**
     * Photo des boutons depuis le tick précédent, puis remise à zéro des
     * appuis mémorisés. À appeler une seule fois par tick.
     *
     * @return nouveau tableau de NB_BOUTONS booléens
     */
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

    /**
     * Si plusieurs touches de chef sont actives, ne garde que la dernière
     * appuyée : le joueur ne reçoit jamais deux changements de chef au même tick.
     */
    private void verifierDernierChef(boolean[] etat) {
        boolean dernierChefActif = dernierChef != -1 && etat[dernierChef];

        etat[BOUTON_PHILIPPE] = false;
        etat[BOUTON_CYRIL] = false;
        etat[BOUTON_NORBERT] = false;
        etat[BOUTON_CHEF4] = false;

        if (dernierChefActif) {
            etat[dernierChef] = true;
        }
    }

    // ----- Clavier -----

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

    @Override
    public synchronized void keyReleased(KeyEvent evenement) {
        int bouton = boutonDeLaTouche(evenement.getKeyCode());
        if (bouton != -1) {
            enfonce[bouton] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent evenement) {
    }

    // ----- Souris (utilisée par l'écran de debug) -----

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

    /** Le clic est déjà pris en compte dans mousePressed. */
    @Override
    public void mouseClicked(MouseEvent evenement) {
    }

    @Override
    public void mouseEntered(MouseEvent evenement) {
    }

    @Override
    public void mouseExited(MouseEvent evenement) {
    }

    /** @return abscisse de la souris dans l'image du jeu (pixels) */
    public synchronized int getSourisX() {
        return sourisX;
    }

    public synchronized int getSourisY() {
        return sourisY;
    }

    /**
     * @return true si le bouton est enfoncé ou a été cliqué depuis le dernier
     *         tick. À lire avant getEtatBoutons(), qui remet le clic à zéro.
     */
    public synchronized boolean isClicSouris() {
        return sourisEnfoncee || clicDepuisDernierTic;
    }
}
