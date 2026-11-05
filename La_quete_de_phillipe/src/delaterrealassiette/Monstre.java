package delaterrealassiette;

public abstract class Monstre extends Entite {
    private int direction = 1;  // à droite=1, à gauche=-1
    private int delaiAvantDegats = 0;
    private static final int DELAI_ENTRE_DEGATS = 60;
    
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
    
     /* ________________________________________________________________
     * ACTION : Avance dans la direction imposée, applique la gravité
     *          et fait demi-tour en cas d'obstacle.
     * [INFO] Entite.deplacerX() met vitesseX à 0 en cas de collision :
     *        c'est ce qui signale le mur.
     * ________________________________________________________________ */
    @Override
    public void mettreAJour() {
        vitesseX = direction * vitesse;

        // Gravité (même logique que Joueur)
        boolean estAuSol = map.collisionHitbox(
                positionX, positionY - 1, hitbox[0], hitbox[1])
                && vitesseY <= 0;
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
                          
}
