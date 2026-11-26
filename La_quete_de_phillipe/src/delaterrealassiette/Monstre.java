package delaterrealassiette;

/**
 * Ennemi qui patrouille et blesse le joueur au contact.
 *
 * Comportement : le monstre avance dans sa direction et fait demi-tour
 * lorsqu'il heurte un mur ou lorsqu'il arrive au bord d'un vide trop profond
 * (plus de CHUTE_MAX pixels). Il ne saute pas ; la gravité et les collisions
 * sont celles d'Entite.
 *
 * Après avoir touché le joueur, il attend delaiEntreDegats ticks avant de
 * pouvoir le blesser à nouveau, pour éviter de vider la vie en un instant.
 *
 * Les différents monstres sont des Monstre paramétrés (vitesse, dégâts,
 * hitbox, sprite) créés par des méthodes de fabrique comme creerRat().
 */
public class Monstre extends Entite {
    private static final int delaiEntreDegats = 60;
    /** Hauteur de chute maximale que le monstre accepte avant de faire demi-tour. */
    private static final int CHUTE_MAX = 96;
    private final int vitesse;
    private final int degats;
    private int delaiAvantDegats;

    /**
     * @param vitesse pixels parcourus par tick
     * @param degats points de vie retirés au joueur à chaque contact
     * @param hitbox {largeur, hauteur} en pixels
     */
    public Monstre(Map map, int positionX, int positionY, int vitesse, int degats,
            int[] hitbox, String cheminImage) {
        super(map, positionX, positionY);
        this.vitesse = vitesse;
        this.degats = degats;
        this.hitbox = hitbox;
        this.cheminImage = cheminImage;
        chargerImage(cheminImage);
    }

    @Override
    public int getVitesse() {
        return vitesse;
    }

    /** Les monstres ne sautent pas. */
    @Override
    public int getForceSaut() {
        return 0;
    }

    /**
     * Sonde verticalement sous la colonne x pour savoir s'il y a un sol à
     * moins de CHUTE_MAX pixels.
     */
    private boolean solAtteignable(int positionX) {
        int yLimite = Math.max(0, positionY - 1 - CHUTE_MAX);
        for (int y = positionY - 1; y >= yLimite; y--) {
            if (map.collisionHitbox(positionX, y, 1, 1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tick de patrouille : regarde le bord avant, fait demi-tour devant un
     * vide (sauf s'il y a une échelle), applique la gravité puis se déplace.
     * Si le déplacement a été bloqué (vitesseX remise à 0 par un mur), le
     * monstre se retourne. L'orientation du sprite sert aussi de direction.
     */
    @Override
    public void mettreAJour() {
        int direction = isRegardeVersLaGauche() ? -1 : 1;
        boolean estAuSol = estAuSol();
        int xAvant = direction > 0
                ? positionX + hitbox[0] - 1 + vitesse : positionX - vitesse;

        boolean echelleDevant = map.chevaucheEchelle(xAvant, positionY,
                hitbox[0], hitbox[1]);
        if (estAuSol && !echelleDevant && !solAtteignable(xAvant)) {
            definirOrientationGauche(direction > 0);
            direction = -direction;
        }

        vitesseX = direction * vitesse;
        if (estAuSol) {
            vitesseY = 0;
        } else {
            vitesseY = Math.max(vitesseY + gravite, vitesseYMin);
        }

        deplacer();
        if (vitesseX == 0) {
            definirOrientationGauche(!isRegardeVersLaGauche());
        }
        if (delaiAvantDegats > 0) {
            delaiAvantDegats--;
        }
    }

    /** Blesse le joueur s'il touche le monstre et que le délai entre deux coups est écoulé. */
    public void infligerDegatsAuContact(Joueur joueur) {
        if (delaiAvantDegats == 0 && enCollisionAvec(joueur)) {
            joueur.modifierPointsDeVie(-degats);
            delaiAvantDegats = delaiEntreDegats;
        }
    }

    /**
     * Fabrique du rat : lent (2 px/tick), 8 dégâts par contact.
     *
     * @param positionX position en pixels (déjà convertie depuis les tuiles par Map)
     */
    public static Monstre creerRat(Map carte, int positionX, int positionY) {
        return new Monstre(carte, positionX, positionY, 2, 8,
                new int[]{32, 24}, "images/sprites/rat.png");
    }
}
