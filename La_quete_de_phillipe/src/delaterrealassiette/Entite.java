package delaterrealassiette;

/**
 *
 * @author Antoine
 */

public abstract class Entite {
    
    // Coordonnées sur la carte (en pixels entiers)
    protected int x;
    protected int y;

    /**
     * Constructeur de base.
     */
    public Entite(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Utilisé par le Dev Map pour positionner l'entité au spawn du niveau.
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // --- GETTERS POUR LES COLLISIONS ---
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // --- CONTRAT OBLIGATOIRE POUR JOUEUR ET MONSTRE ---
    public abstract int[] getHitbox();
    public abstract int getVitesse();
    public abstract int getForceSaut();
    public abstract String getCheminSprite();
    
    /**
     * Boucle de jeu (calcul du mouvement, physique)
     */
    public abstract void mettreAJour();
}