package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;


/* ________________________________________________________________
 * ACTION : Représente un levier commandant un mécanisme du niveau.
 * ________________________________________________________________ */
public class Levier extends Entite {
    private final int idMecanisme;
    private boolean estActive;
    private static final String cheminImageLevier = "images/levier.png";

    /* ________________________________________________________________
     * ACTION : Initialise un levier inactif à sa position en pixels.
     * ________________________________________________________________ */
    public Levier(Map map, int idMecanisme, int positionX, int positionY) {
        super(map, positionX, positionY);
        this.idMecanisme = idMecanisme;
        int tailleTuile = map.getTailleTuile();
        this.hitbox = new int[]{tailleTuile, tailleTuile};
    }

    /* ________________________________________________________________
     * ACTION : Retourne l'identifiant du mécanisme associé.
     * ________________________________________________________________ */
    public int getIdMecanisme() { return idMecanisme; }

    /* ________________________________________________________________
     * ACTION : Indique si le levier est actif.
     * ________________________________________________________________ */
    public boolean isActive() { return estActive; }

    /* ________________________________________________________________
     * ACTION : Définit l'état du levier.
     * ________________________________________________________________ */
    public void definirActif(boolean actif) { estActive = actif; }

    /* ________________________________________________________________
     * ACTION : Bascule l'état actif du levier.
     * ________________________________________________________________ */
    public void basculer() { estActive = !estActive; }

    /* ________________________________________________________________
     * ACTION : Indique si le joueur est en contact ou à portée du levier.
     * ________________________________________________________________ */
    public boolean estAProximite(Joueur joueur) {
        int[] hitboxJoueur = joueur.getHitbox();
        int marge = map.getTailleTuile();
        return joueur.getPositionX() - marge < positionX + hitbox[0]
                && joueur.getPositionX() + hitboxJoueur[0] + marge > positionX
                && joueur.getPositionY() - marge < positionY + hitbox[1]
                && joueur.getPositionY() + hitboxJoueur[1] + marge > positionY;
    }

    /* ________________________________________________________________
     * ACTION : Retourne une hitbox de levier non solide.
     * ________________________________________________________________ */
    @Override public int[] getHitbox() { return hitbox; }
    /* ________________________________________________________________
     * ACTION : Retourne la vitesse nulle du levier.
     * ________________________________________________________________ */
    @Override public int getVitesse() { return 0; }
    /* ________________________________________________________________
     * ACTION : Retourne une impulsion nulle.
     * ________________________________________________________________ */
    @Override public int getForceSaut() { return 0; }
    /* ________________________________________________________________
     * ACTION : Retourne le chemin du sprite du levier.
     * ________________________________________________________________ */
    @Override public String getCheminImage() { return cheminImageLevier; }
    /* ________________________________________________________________
     * ACTION : Ne réalise aucun déplacement.
     * ________________________________________________________________ */
    @Override public void mettreAJour() { }

    /* ________________________________________________________________
     * ACTION : Oriente le sprite selon l'état actif du levier.
     * ________________________________________________________________ */
    @Override
    protected boolean estMiroir() {
        return estActive;
    }

    /* ________________________________________________________________
     * ACTION : Dessine la couleur de remplacement du levier.
     * ________________________________________________________________ */
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
