package delaterrealassiette;

public class Monstre extends Entite {
    private static final int DELAI_ENTRE_DEGATS = 60; // en tics
    private static final int CHUTE_MAX = 96; // pixels, soit 3 tuiles de 32 : au-delà, on considère que c'est un trou

    private final int vitesse;
    private final int degats;
    private final int[] hitbox;
    private final String cheminImage;

    private int direction = 1;          // 1 = droite, -1 = gauche
    private int delaiAvantDegats = 0;
    
    public Monstre(Map map, int positionX, int positionY,int vitesse, int degats, int[] hitbox, String cheminImage) {
        super(map, positionX, positionY);
        this.vitesse = vitesse;
        this.degats = degats;
        this.hitbox = hitbox;
        this.cheminImage = cheminImage;
    }
    
    
    
    @Override
    public int[] getHitbox() {
        return hitbox;
    }

    @Override
    public int getVitesse() {
        return vitesse;
    }

    @Override
    public int getForceSaut() {
        return 0; // parce que le monstre ne saute pas
    }

    @Override
    public String getCheminImage() {
        return cheminImage;
    }
    
    /* Place le monstre sur un sol libre, en reculant d'une tuile vers la
    * gauche tant que la colonne est un mur ou un trou. */
    private void placerSurSol(int xDepart) {
        for (int x = xDepart; x >= 0; x -= map.getTailleTuile()) {
            for (int y = map.getHauteurPixels() - hitbox[1]; y >= 1; y--) {
                if (!map.collisionHitbox(x, y, hitbox[0], hitbox[1])
                        && map.collisionHitbox(x, y - 1, hitbox[0], hitbox[1])) {
                    definirPosition(x, y);
                    return;
                }
            }
        }
    }
    
    /*
    Pour vérifier si le monstre doit faire demi-tour ou s'il peut se placer sur la plateforme du dessous
    */
    
    private boolean solAtteignable(int x) {
    int yLimite = Math.max(0, positionY - 1 - CHUTE_MAX);
    for (int y = positionY - 1; y >= yLimite; y--) {
        if (map.collisionHitbox(x, y, 1, 1)) {
            return true;
        }
    }
    return false;
    }
    
    
     /* ________________________________________________________________
     * ACTION : Avance dans la direction imposée, applique la gravité
     *          et fait demi-tour en cas d'obstacle/bord de map.
     * [INFO] Entite.deplacerX() met vitesseX à 0 en cas de collision :
     *        c'est ce qui signale le mur. Il fait demi-tour devant un trou, 
              sauf s’il y a un sol à moins de 96 px en dessous : dans ce cas il tombe dessus.
     * ________________________________________________________________ */
    
    @Override
    public void mettreAJour() {
        boolean estAuSol = map.collisionHitbox(
            positionX, positionY - 1, hitbox[0], hitbox[1])
            && vitesseY <= 0;

    // Pixel juste sous le pied avant, après le déplacement prévu
        int xAvant = (direction > 0)
            ? positionX + hitbox[0] - 1 + vitesse
            : positionX - vitesse;

    // Au sol, mais plus de sol devant : on traite le trou comme un mur
        if (estAuSol && !solAtteignable(xAvant)) {
        direction = -direction;
        }

        vitesseX = direction * vitesse;

        if (estAuSol) {
            vitesseY = 0;
        } else {
            vitesseY = Math.max(vitesseY + gravite, vitesseYMin);
        }

        deplacer();

        if (vitesseX == 0) {        // bloqué par un mur
            direction = -direction;
        }

        if (delaiAvantDegats > 0) {
            delaiAvantDegats--;
        }
    }
    
     /* ________________________________________________________________
     * ACTION : Indique si la hitbox du monstre chevauche celle d'une entité.
     * [INFO] Origine = coin inférieur gauche, y vers le haut.
     * ________________________________________________________________ */
    public boolean enCollisionAvec(Entite autre) {
        int[] h = autre.getHitbox();
        return positionX < autre.getPositionX() + h[0]
                && positionX + hitbox[0] > autre.getPositionX()
                && positionY < autre.getPositionY() + h[1]
                && positionY + hitbox[1] > autre.getPositionY();
    }

    /* ________________________________________________________________
     * ACTION : Retire des points de vie au joueur s'il y a collision
     *          et que le délai est écoulé.
     * ________________________________________________________________ */
    public void testerCollision(Joueur joueur) {
        if (delaiAvantDegats == 0 && enCollisionAvec(joueur)) {
            joueur.modifierPointsDeVie(-degats);
            delaiAvantDegats = DELAI_ENTRE_DEGATS;
        }
    }
    
    /* 
    * Création monstre de premier type
    */
    public static Monstre creerType1(Map map) {
        Monstre m = new Monstre(map, 0, 0, 3, 10, new int[]{32, 32}, "/sprites/monstre1.png");
        m.placerSurSol(map.getLargeurPixels() - m.hitbox[0] - map.getTailleTuile());
        return m;
    }
                
}
