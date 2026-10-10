package delaterrealassiette;

public abstract class Entite {
    static private final int HITBOX_LARGEUR = 0;
    static private final int HITBOX_HAUTEUR = 1;

    protected int id;
    protected int x;
    protected int y;
    protected int pv;
    protected int pvMax;
    protected int pas;
    protected int porteeSaut;
    protected int[] hitbox = new int[2];
    private boolean modifie;

    public abstract void mettreAJour();

    public void subirDegats(int degats) {
        // TODO
    }

    public boolean estVivant() {
        // TODO
        return false;
    }

    public int getId() {
        // TODO
        return 0;
    }

    public int getX() {
        // TODO
        return 0;
    }

    public int getY() {
        // TODO
        return 0;
    }

    public int getHitbox(int index) {
        // TODO
        return 0;
    }

    public int getHitboxLargeur() {
        // TODO
        return 0;
    }

    public int getHitboxHauteur() {
        // TODO
        return 0;
    }

    public int getPv() {
        // TODO
        return 0;
    }

    public int getPvMax() {
        // TODO
        return 0;
    }

    public int getPas() {
        // TODO
        return 0;
    }

    public int getPorteeSaut() {
        // TODO
        return 0;
    }

    public void setPosition(int x, int y) {
        // TODO
    }

    public void setPv(int pv) {
        // TODO
    }

    protected void marquerModifie() {
        // TODO
    }

    public boolean estModifie() {
        // TODO
        return false;
    }

    protected void effacerModifie() {
        // TODO
    }
}
