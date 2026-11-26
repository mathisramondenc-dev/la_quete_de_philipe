package delaterrealassiette;

/** Instantané d'un participant distant publié par SynchronisationBDD. */
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
    public String getCheminImage() {
        String nom = avatar;
        if (!"Etchebest".equals(nom) && !"Lignac".equals(nom) && !"Tarayre".equals(nom) && !"Maite".equals(nom)) nom = "Etchebest";
        return "images/sprites/" + nom.toLowerCase(java.util.Locale.ROOT) + ".png";
    }
    public int getLargeur() { return "Lignac".equals(avatar) ? 28 : ("Tarayre".equals(avatar) ? 18 : 32); }
    public int getHauteur() { return "Lignac".equals(avatar) ? 60 : ("Tarayre".equals(avatar) ? 30 : 64); }
}
