// Avatar.java
package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Regroupe les caractéristiques d'un chef jouable.
 * [INFO] La hitbox contient la largeur puis la hauteur.
 * [DEPENDANCE] Joueur utilise les caractéristiques de l'avatar actif.
 * ________________________________________________________________ */
public class Avatar {
    private final String nom;
    private final int vitesseDeplacementX;
    private final int forceSaut;
    private final int[] hitbox;
    private final String cheminImage;

    /* ________________________________________________________________
     * ACTION : Initialise les caractéristiques d'un avatar.
     * [INFO] La hitbox contient la largeur puis la hauteur.
     * ________________________________________________________________ */
    public Avatar(String nom, int vitesseDeplacementX,
            int forceSaut, int[] hitbox, String cheminImage) {
        this.nom = nom;
        this.vitesseDeplacementX = vitesseDeplacementX;
        this.forceSaut = forceSaut;
        this.hitbox = hitbox;
        this.cheminImage = cheminImage;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le nom de l'avatar.
     * ________________________________________________________________ */
    public String getNom() {
        return nom;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la vitesse de déplacement sur l'axe x.
     * ________________________________________________________________ */
    public int getVitesseDeplacementX() {
        return vitesseDeplacementX;
    }

    /* ________________________________________________________________
     * ACTION : Retourne l'impulsion du saut.
     * ________________________________________________________________ */
    public int getForceSaut() {
        return forceSaut;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hitbox.
     * ________________________________________________________________ */
    public int[] getHitbox() {
        return hitbox;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le chemin de l'image.
     * ________________________________________________________________ */
    public String getCheminImage() {
        return cheminImage;
    }
}