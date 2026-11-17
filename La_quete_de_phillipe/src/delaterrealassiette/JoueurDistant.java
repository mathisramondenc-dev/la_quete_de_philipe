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
}
