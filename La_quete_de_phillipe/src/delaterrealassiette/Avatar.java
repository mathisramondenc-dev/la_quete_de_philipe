package delaterrealassiette;

/**
 *
 * @author Antoine
 */

public class Avatar {
    
    // Identité de l'avatar
    private String nom;
    
    // Statistiques de déplacement
    private int vitesse;
    private int forceSaut;
    
    // Hitbox : liste contenant 2 entiers -> [largeur, hauteur]
    private int[] hitbox;

    //Chemin du Sprite
    private String cheminSprite;
    /**
     * Constructeur de l'Avatar
     */
    public Avatar(String nom, int vitesse, int forceSaut, int[] hitbox, String cheminSprite) {
        this.nom = nom;
        this.vitesse = vitesse;
        this.forceSaut = forceSaut;
        this.hitbox = hitbox;
        this.cheminSprite = cheminSprite;
    }

    // --- GETTERS ---

    public String getNom() {
        return nom;
    }

    public int getVitesse() {
        return vitesse;
    }

    public int getForceSaut() {
        return forceSaut;
    }

    public int[] getHitbox() {
        return hitbox;
    }
    
    public String getCheminSprite() {
        return cheminSprite;
    }
}