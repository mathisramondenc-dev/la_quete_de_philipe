package delaterrealassiette;

import java.awt.Graphics2D;

/**
 * Porte commandée par un levier : fermée, elle est visible et bloque les
 * entités ; ouverte, elle disparaît et se traverse.
 *
 * Identifiants :
 * - idMecanisme relie la porte au levier qui porte le même numéro ;
 * - idPorte distingue chaque ligne PORTE du fichier de niveau, afin que tous
 *   les blocs d'une même porte s'ouvrent et se ferment ensemble.
 *
 * L'état se fait en deux temps, piloté par Map.mettreAJourMecanismes() :
 * le levier donne d'abord l'état « demandé », puis la Map l'applique. Une
 * ouverture est immédiate ; une fermeture attend que plus aucune entité ne
 * se trouve dans la porte, pour ne coincer personne dans le mur.
 *
 * Le levier inverse l'état de départ : une porte déclarée FERMEE s'ouvre
 * quand le levier est actif, une porte OUVERTE se ferme.
 */
public class BlocPorte extends ObjetInteractif {
    private final int idMecanisme;
    private final int idPorte;
    private final boolean fermeeInitialement;
    private boolean fermee;
    private boolean fermetureDemandee;

    /**
     * @param idMecanisme numéro du levier qui commande la porte
     * @param idPorte numéro de la ligne PORTE dont provient ce bloc
     * @param fermeeInitialement état lorsque le levier est inactif
     */
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

    public int getIdMecanisme() { return idMecanisme; }

    public int getIdPorte() { return idPorte; }

    public boolean isFermeeInitialement() { return fermeeInitialement; }

    public boolean isFermee() { return fermee; }

    /** Enregistre l'état voulu par le levier ; il sera appliqué par definirFerme(). */
    public void definirFermetureDemandee(boolean demandee) { fermetureDemandee = demandee; }

    public boolean estFermetureDemandee() { return fermetureDemandee; }

    /** Applique réellement l'état : une porte fermée devient solide. */
    public void definirFerme(boolean fermee) {
        this.fermee = fermee;
        definirCollisionJoueur(fermee);
    }

    /** Une porte ouverte n'est pas dessinée. */
    @Override
    protected boolean estVisible() {
        return fermee;
    }

    /**
     * La porte n'a pas de sprite propre : elle est toujours dessinée avec la
     * tuile n°5 du tileset du niveau, répétée sur toute sa surface.
     */
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
