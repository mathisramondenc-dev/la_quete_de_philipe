package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Gère le déplacement, le rendu et les dégâts d'un monstre.
 * [INFO] L'orientation visuelle est conservée par ElementDeJeu.
 * [DEPENDANCE] Entite applique la gravité et les collisions avec Map.
 * ________________________________________________________________ */
public class Monstre extends Entite {
    private static final int delaiEntreDegats = 60;
    private static final int CHUTE_MAX = 96;
    private final int vitesse;
    private final int degats;
    private int delaiAvantDegats;

    /* ________________________________________________________________
     * ACTION : Initialise un monstre à une position donnée.
     * [DEPENDANCE] Jeu et Map fournissent la carte et les coordonnées.
     * ________________________________________________________________ */
    public Monstre(Map map, int positionX, int positionY, int vitesse, int degats,
            int[] hitbox, String cheminImage) {
        super(map, positionX, positionY);
        this.vitesse = vitesse;
        this.degats = degats;
        this.hitbox = hitbox;
        this.cheminImage = cheminImage;
        chargerImage(cheminImage);
    }

    /* ________________________________________________________________
     * ACTION : Retourne la vitesse horizontale du monstre.
     * ________________________________________________________________ */
    @Override
    public int getVitesse() {
        return vitesse;
    }

    /* ________________________________________________________________
     * ACTION : Indique que le monstre ne peut pas sauter.
     * ________________________________________________________________ */
    @Override
    public int getForceSaut() {
        return 0;
    }

    /* ________________________________________________________________
     * ACTION : Vérifie si le sol est atteignable sous le bord avant.
     * ________________________________________________________________ */
    private boolean solAtteignable(int positionX) {
        int yLimite = Math.max(0, positionY - 1 - CHUTE_MAX);
        for (int y = positionY - 1; y >= yLimite; y--) {
            if (map.collisionHitbox(positionX, y, 1, 1)) {
                return true;
            }
        }
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Applique la gravité et patrouille en évitant les trous.
     * [INFO] Le sens du déplacement et le miroir utilisent l'orientation commune.
     * [DEPENDANCE] Entite résout les déplacements sur les axes.
     * ________________________________________________________________ */
    @Override
    public void mettreAJour() {
        int direction = isRegardeVersLaGauche() ? -1 : 1;
        boolean estAuSol = map.collisionHitbox(
                positionX, positionY - 1, hitbox[0], hitbox[1]) && vitesseY <= 0;
        int xAvant = direction > 0
                ? positionX + hitbox[0] - 1 + vitesse : positionX - vitesse;

        if (estAuSol && !solAtteignable(xAvant)) {
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

    /* ________________________________________________________________
     * ACTION : Inflige des dégâts au joueur au contact du monstre.
     * [INFO] Utilise le délai temporaire commun entre deux dégâts.
     * [DEPENDANCE] ElementDeJeu fournit enCollisionAvec pour comparer les hitbox.
     * ________________________________________________________________ */
    public void infligerDegatsAuContact(Joueur joueur) {
        if (delaiAvantDegats == 0 && enCollisionAvec(joueur)) {
            joueur.modifierPointsDeVie(-degats);
            delaiAvantDegats = delaiEntreDegats;
        }
    }

    /* ________________________________________________________________
     * ACTION : Crée un rat à la position déclarée dans le niveau.
     * [DEPENDANCE] La carte fournit les coordonnées déjà converties en pixels.
     * ________________________________________________________________ */
    public static Monstre creerRat(Map carte, int positionX, int positionY) {
        return new Monstre(carte, positionX, positionY, 2, 8,
                new int[]{32, 24}, "images/sprites/rat.png");
    }
}
