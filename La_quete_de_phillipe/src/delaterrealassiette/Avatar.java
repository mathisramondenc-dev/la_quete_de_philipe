package delaterrealassiette;

/**
 * Fiche de caractéristiques d'un chef jouable (Etchebest, Lignac, Tarayre,
 * Maite) : nom, vitesse, force de saut, hitbox et sprite.
 *
 * Objet immuable sans logique : le Joueur garde une instance par chef et lit
 * celle du chef incarné. Ajouter un chef revient donc à créer un nouvel
 * Avatar dans Joueur, sans toucher à la physique. Les compétences propres à
 * chaque chef sont gérées par Joueur à partir du nom.
 */
public class Avatar {
    private final String nom;
    private final int vitesseDeplacementX;
    private final int forceSaut;
    private final int[] hitbox;
    private final String cheminImage;

    /**
     * @param vitesseDeplacementX vitesse horizontale, en pixels par tick
     * @param forceSaut vitesse verticale donnée au départ d'un saut
     * @param dimensionsHitbox {largeur, hauteur} en pixels
     * @param cheminImage chemin du sprite
     */
    public Avatar(String nom, int vitesseDeplacementX,
            int forceSaut, int[] dimensionsHitbox, String cheminImage) {
        this.nom = nom;
        this.vitesseDeplacementX = vitesseDeplacementX;
        this.forceSaut = forceSaut;
        this.hitbox = dimensionsHitbox;
        this.cheminImage = cheminImage;
    }

    public String getNom() {
        return nom;
    }

    public int getVitesseDeplacementX() {
        return vitesseDeplacementX;
    }

    public int getForceSaut() {
        return forceSaut;
    }

    /** @return {largeur, hauteur} en pixels */
    public int[] getHitbox() {
        return hitbox;
    }

    public String getCheminImage() {
        return cheminImage;
    }
}
