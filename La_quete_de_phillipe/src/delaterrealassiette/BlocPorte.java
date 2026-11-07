package delaterrealassiette;

import java.awt.Graphics2D;

/* ________________________________________________________________
 * ACTION : Représente un bloc de porte piloté par un levier.
 * ________________________________________________________________ */
public class BlocPorte extends Entite {
    private final int idMecanisme;
    private final int idPorte;
    private final int[] hitbox;
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
     * ACTION : Teste le chevauchement avec une hitbox rectangulaire.
     * ________________________________________________________________ */
    public boolean chevaucheRectangle(int x, int y, int largeur, int hauteur) {
        return x < positionX + hitbox[0] && x + largeur > positionX
                && y < positionY + hitbox[1] && y + hauteur > positionY;
    }

    /* ________________________________________________________________
     * ACTION : Indique si ce bloc chevauche une entité.
     * ________________________________________________________________ */
    public boolean enCollisionAvec(Entite entite) {
        int[] hitboxEntite = entite.getHitbox();
        return chevaucheRectangle(entite.getPositionX(), entite.getPositionY(),
                hitboxEntite[0], hitboxEntite[1]);
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hitbox rectangulaire du bloc.
     * ________________________________________________________________ */
    @Override public int[] getHitbox() { return hitbox; }
    /* ________________________________________________________________
     * ACTION : Retourne une vitesse nulle.
     * ________________________________________________________________ */
    @Override public int getVitesse() { return 0; }
    /* ________________________________________________________________
     * ACTION : Retourne une impulsion nulle.
     * ________________________________________________________________ */
    @Override public int getForceSaut() { return 0; }
    /* ________________________________________________________________
     * ACTION : Retourne un chemin d'image vide.
     * ________________________________________________________________ */
    @Override public String getCheminImage() { return ""; }
    /* ________________________________________________________________
     * ACTION : Ne réalise aucun déplacement.
     * ________________________________________________________________ */
    @Override public void mettreAJour() { }

    /* ________________________________________________________________
     * ACTION : Dessine les tuiles pleines de la porte lorsqu'elle est fermée.
     * ________________________________________________________________ */
    @Override public void dessiner(Graphics2D contexte, int x, int y) {
        if (!fermee) {
            return;
        }
        int tailleTuile = map.getTailleTuile();
        for (int ligne = 0; ligne < hitbox[1]; ligne += tailleTuile) {
            for (int colonne = 0; colonne < hitbox[0]; colonne += tailleTuile) {
                map.dessinerTuile(contexte, 5, x + colonne, y + ligne,
                        Math.min(tailleTuile, hitbox[0] - colonne),
                        Math.min(tailleTuile, hitbox[1] - ligne));
            }
        }
    }
}
