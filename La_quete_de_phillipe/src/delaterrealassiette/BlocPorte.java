package delaterrealassiette;

import java.awt.Graphics2D;

/* ________________________________________________________________
 * ACTION : Représente un bloc de porte piloté par un levier.
 * ________________________________________________________________ */
public class BlocPorte extends ObjetInteractif {
    private final int idMecanisme;
    private final int idPorte;
    private final boolean fermeeInitialement;
    private boolean fermee;
    private boolean fermetureDemandee;

    /* ________________________________________________________________
     * ACTION : Initialise une porte avec ses identifiants et ses dimensions.
     * ________________________________________________________________ */
    public BlocPorte(Map map, int idMecanisme, int idPorte, int positionX, int positionY,
            int largeur, int hauteur, boolean fermeeInitialement) {
        super(map, positionX, positionY);
        this.idMecanisme = idMecanisme;
        this.idPorte = idPorte;
        this.hitbox = new int[]{largeur, hauteur};
        this.fermeeInitialement = fermeeInitialement;
        this.fermee = fermeeInitialement;
        definirCollisionJoueur(fermee);
    }

    /* ________________________________________________________________
     * ACTION : Retourne l'identifiant du mécanisme associé.
     * ________________________________________________________________ */
    public int getIdMecanisme() { return idMecanisme; }

    /* ________________________________________________________________
     * ACTION : Retourne l’identifiant de la porte déclarée dans le niveau.
     * [INFO] Tous les blocs d’une même ligne PORTE partagent cet identifiant.
     * [DEPENDANCE] Map attribue cet identifiant lors du chargement du niveau.
     * ________________________________________________________________ */
    public int getIdPorte() { return idPorte; }

    /* ________________________________________________________________
     * ACTION : Retourne l'état initial fermé déclaré dans le niveau.
     * ________________________________________________________________ */
    public boolean isFermeeInitialement() { return fermeeInitialement; }

    /* ________________________________________________________________
     * ACTION : Indique si le bloc est fermé.
     * ________________________________________________________________ */
    public boolean isFermee() { return fermee; }

    /* ________________________________________________________________
     * ACTION : Enregistre l'état demandé par le levier.
     * ________________________________________________________________ */
    public void definirFermetureDemandee(boolean demandee) { fermetureDemandee = demandee; }

    /* ________________________________________________________________
     * ACTION : Indique si une fermeture est demandée.
     * ________________________________________________________________ */
    public boolean estFermetureDemandee() { return fermetureDemandee; }

    /* ________________________________________________________________
     * ACTION : Applique l'état de fermeture et sa collision.
     * ________________________________________________________________ */
    public void definirFerme(boolean fermee) {
        this.fermee = fermee;
        definirCollisionJoueur(fermee);
    }

    /* ________________________________________________________________
     * ACTION : Affiche la porte uniquement lorsqu'elle est fermée.
     * ________________________________________________________________ */
    @Override
    protected boolean estVisible() {
        return fermee;
    }

    /* ________________________________________________________________
     * ACTION : Dessine les tuiles pleines de la porte.
     * [DEPENDANCE] Map fournit la tuile de porte et sa taille.
     * ________________________________________________________________ */
    @Override
    protected void dessinerImageManquante(Graphics2D contexte, int positionEcranX,
            int positionEcranY, int largeurZone, int hauteurZone) {
        int tailleTuile = map.getTailleTuile();
        for (int ligne = 0; ligne < hauteurZone; ligne += tailleTuile) {
            for (int colonne = 0; colonne < largeurZone; colonne += tailleTuile) {
                map.dessinerTuile(contexte, 5, positionEcranX + colonne, positionEcranY + ligne,
                        Math.min(tailleTuile, largeurZone - colonne),
                        Math.min(tailleTuile, hauteurZone - ligne));
            }
        }
    }
}
