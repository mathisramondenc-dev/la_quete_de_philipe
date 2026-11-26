package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;


/**
 * Levier qui commande une ou plusieurs portes du niveau.
 *
 * Le lien levier/porte se fait par un numéro de mécanisme : dans le fichier
 * de niveau, "LEVIER 1 ..." commande toutes les lignes "PORTE 1 ...". Le
 * levier ne connaît pas ses portes ; il expose seulement son état (actif ou
 * non), que la Map relit à chaque tick pour ouvrir ou fermer les portes.
 *
 * Seul Tarayre peut l'actionner, à une tuile de distance au plus.
 * L'état est représenté visuellement en retournant le sprite.
 */
public class Levier extends ObjetInteractif {
    private final int idMecanisme;
    private boolean estActive;
    private static final String cheminImageLevier = "images/levier.png";

    /**
     * Crée un levier inactif d'une tuile de côté.
     *
     * @param idMecanisme numéro partagé avec les portes commandées
     */
    public Levier(Map map, int idMecanisme, int positionX, int positionY) {
        super(map, positionX, positionY);
        this.idMecanisme = idMecanisme;
        this.cheminImage = cheminImageLevier;
        int tailleTuile = map.getTailleTuile();
        this.hitbox = new int[]{tailleTuile, tailleTuile};
    }

    public int getIdMecanisme() { return idMecanisme; }

    public boolean isActive() { return estActive; }

    /** Impose l'état (utilisé pour appliquer l'état reçu en multijoueur). */
    public void definirActif(boolean actif) { estActive = actif; }

    public void basculer() { estActive = !estActive; }

    /**
     * @return true si le joueur touche le levier ou en est à moins d'une tuile
     *         (sa hitbox est agrandie d'une tuile de chaque côté)
     */
    public boolean estAProximite(Joueur joueur) {
        int[] hitboxJoueur = joueur.getHitbox();
        int marge = map.getTailleTuile();
        return joueur.getPositionX() - marge < positionX + hitbox[0]
                && joueur.getPositionX() + hitboxJoueur[0] + marge > positionX
                && joueur.getPositionY() - marge < positionY + hitbox[1]
                && joueur.getPositionY() + hitboxJoueur[1] + marge > positionY;
    }

    /** Levier actif = sprite retourné. */
    @Override
    protected boolean estMiroir() {
        return estActive;
    }

    /** Sans image : carré vert si actif, orange sinon. */
    @Override
    protected void dessinerImageManquante(Graphics2D contexte, int positionEcranX,
            int positionEcranY, int largeurZone, int hauteurZone) {
        Color couleurPrecedente = contexte.getColor();
        contexte.setColor(estActive
                ? new Color(50, 220, 90) : new Color(220, 150, 40));
        contexte.fillRect(positionEcranX, positionEcranY, largeurZone, hauteurZone);
        contexte.setColor(couleurPrecedente);
    }
}
