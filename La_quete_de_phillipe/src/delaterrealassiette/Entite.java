package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Centralise la position et les déplacements des entités.
 * [INFO] L'origine de la hitbox est le coin inférieur gauche.
 * [DEPENDANCE] La map est créée par Jeu et fournit collisionHitbox.
 * ________________________________________________________________ */
public abstract class Entite extends ElementDeJeu {
    protected int vitesseX;
    protected int vitesseY;
    protected int gravite = -6;
    protected int vitesseYMin = -24;
    protected final Map map;
    protected boolean collisionJoueur = false;

    /* ________________________________________________________________
     * ACTION : Initialise la position et la map de l'entité.
     * [INFO] La map n'est jamais recréée par l'entité.
     * [DEPENDANCE] Jeu transmet son unique instance de Map.
     * ________________________________________________________________ */
    public Entite(Map map, int positionX, int positionY) {
        super(positionX, positionY);
        this.map = map;
    }

    /* ________________________________________________________________
     * ACTION : Initialise l'entité à l'origine.
     * [DEPENDANCE] Jeu transmet son unique instance de Map.
     * ________________________________________________________________ */
    public Entite(Map map) {
        this(map, 0, 0);
    }

    /* ________________________________________________________________
     * ACTION : Retourne la taille de tuile fournie par la carte.
     * [DEPENDANCE] Map fournit la taille des tuiles du niveau.
     * ________________________________________________________________ */
    @Override
    protected int getTailleTuileDessin() {
        return map.getTailleTuile();
    }

    /* ________________________________________________________________
     * ACTION : Définit si cette entité est solide pour les joueurs et les monstres.
     * ________________________________________________________________ */
    public void definirCollisionJoueur(boolean collisionJoueur) {
        this.collisionJoueur = collisionJoueur;
    }

    /* ________________________________________________________________
     * ACTION : Indique si cette entité est solide pour les joueurs et les monstres.
     * ________________________________________________________________ */
    public boolean isCollisionJoueur() {
        return collisionJoueur;
    }

    /* ________________________________________________________________
     * ACTION : Applique les déplacements sur les deux axes.
     * ________________________________________________________________ */
    public void deplacer() {
        deplacerX();
        deplacerY();
    }

    /* ________________________________________________________________
     * ACTION : Déplace l'entité sur l'axe x pixel par pixel.
     * [INFO] Une collision annule la vitesse sur l'axe x.
     * [DEPENDANCE] La map fournit collisionHitbox.
     * ________________________________________________________________ */
    protected void deplacerX() {
        int[] dimensionsHitbox = getHitbox();
        int largeurHitbox = dimensionsHitbox[0];
        int hauteurHitbox = dimensionsHitbox[1];
        int direction = Integer.signum(vitesseX);
        int distance = Math.abs(vitesseX);

        for (int pixel = 0; pixel < distance; pixel++) {
            int prochainePositionX = positionX + direction;

            if (map.collisionHitbox(
                    prochainePositionX, positionY, largeurHitbox, hauteurHitbox)) {
                vitesseX = 0;
                break;
            }

            positionX = prochainePositionX;
        }
    }

    /* ________________________________________________________________
     * ACTION : Déplace l'entité sur l'axe y pixel par pixel.
     * [INFO] Une collision annule la vitesse sur l'axe y.
     * [DEPENDANCE] La map fournit collisionHitbox.
     * ________________________________________________________________ */
    protected void deplacerY() {
        int[] dimensionsHitbox = getHitbox();
        int largeurHitbox = dimensionsHitbox[0];
        int hauteurHitbox = dimensionsHitbox[1];
        int direction = Integer.signum(vitesseY);
        int distance = Math.abs(vitesseY);

        for (int pixel = 0; pixel < distance; pixel++) {
            int prochainePositionY = positionY + direction;

            if (map.collisionHitbox(
                    positionX, prochainePositionY, largeurHitbox, hauteurHitbox)) {
                vitesseY = 0;
                break;
            }

            positionY = prochainePositionY;
        }
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hitbox sous la forme largeur puis hauteur.
     * ________________________________________________________________ */
    public abstract int[] getHitbox();

    /* ________________________________________________________________
     * ACTION : Retourne la vitesse de déplacement.
     * ________________________________________________________________ */
    public abstract int getVitesse();

    /* ________________________________________________________________
     * ACTION : Retourne l'impulsion du saut.
     * ________________________________________________________________ */
    public abstract int getForceSaut();

    /* ________________________________________________________________
     * ACTION : Retourne le chemin de l'image.
     * ________________________________________________________________ */
    public abstract String getCheminImage();

    /* ________________________________________________________________
     * ACTION : Actualise les intentions et les déplacements.
     * ________________________________________________________________ */
    public abstract void mettreAJour();

    /* ________________________________________________________________
     * ACTION : Définit la position sur l'axe x.
     * ________________________________________________________________ */
    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    /* ________________________________________________________________
     * ACTION : Définit la position sur l'axe y.
     * ________________________________________________________________ */
    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }
    
    
    
    

}
