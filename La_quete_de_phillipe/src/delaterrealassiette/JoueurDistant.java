package delaterrealassiette;

/**
 * Photo, à un instant donné, d'un autre joueur de la partie multijoueur :
 * pseudo, chef incarné et position, tels que lus dans la base de données.
 *
 * Simple objet en lecture seule, recréé à chaque synchronisation et utilisé
 * uniquement pour l'affichage. La taille et le sprite sont déduits du nom du
 * chef (un nom inconnu est affiché comme Etchebest).
 */
public final class JoueurDistant {
    private final String pseudo, avatar;
    private final int x, y;
    public JoueurDistant(String pseudo, String avatar, int x, int y) {
        this.pseudo = pseudo; this.avatar = avatar; this.x = x; this.y = y;
    }
    public String getPseudo() { return pseudo; }
    public String getAvatar() { return avatar; }
    public int getX() { return x; }
    public int getY() { return y; }
    /** @return chemin du sprite du chef, Etchebest par défaut */
    public String getCheminImage() {
        String nom = avatar;
        if (!"Etchebest".equals(nom) && !"Lignac".equals(nom) && !"Tarayre".equals(nom) && !"Maite".equals(nom)) nom = "Etchebest";
        return "images/sprites/" + nom.toLowerCase(java.util.Locale.ROOT) + ".png";
    }
    // Dimensions identiques aux hitbox déclarées dans Joueur
    public int getLargeur() { return "Lignac".equals(avatar) ? 28 : ("Tarayre".equals(avatar) ? 18 : 32); }
    public int getHauteur() { return "Lignac".equals(avatar) ? 60 : ("Tarayre".equals(avatar) ? 30 : 64); }
}
