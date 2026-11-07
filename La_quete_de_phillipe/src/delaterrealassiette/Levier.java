package delaterrealassiette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/* ____ ACTION : Représente un levier commandant un mécanisme du niveau. ____ */
public class Levier extends Entite {
    private final int idMecanisme;
    private boolean estActive;
    private final int[] hitbox;
    private static final String cheminImageLevier = "images/levier.png";
    private static final BufferedImage imageLevierNormale = chargerImageLevier();
    private static final BufferedImage imageLevierRetournee = retournerImageLevier(imageLevierNormale);

    /* ____ ACTION : Charge le sprite du levier depuis le chemin configuré.
     * [INFO] Une image absente conserve le dessin de remplacement. ____ */
    private static BufferedImage chargerImageLevier() {
        File fichier = new File(cheminImageLevier);
        File repertoire = new File(System.getProperty("user.dir"));
        while (!fichier.isFile() && repertoire != null) {
            fichier = new File(repertoire, cheminImageLevier);
            repertoire = repertoire.getParentFile();
        }
        try {
            return fichier.isFile() ? ImageIO.read(fichier) : null;
        } catch (IOException exception) {
            return null;
        }
    }

    /* ____ ACTION : Prépare une seule fois le sprite miroir du levier.
     * [DEPENDANCE] Le sprite normal est chargé par chargerImageLevier. ____ */
    private static BufferedImage retournerImageLevier(BufferedImage imageLevier) {
        if (imageLevier == null) {
            return null;
        }
        BufferedImage imageRetournee = new BufferedImage(
                imageLevier.getWidth(), imageLevier.getHeight(), BufferedImage.TYPE_INT_ARGB);
        AffineTransform transformation = AffineTransform.getScaleInstance(-1, 1);
        transformation.translate(-imageLevier.getWidth(), 0);
        Graphics2D contexte = imageRetournee.createGraphics();
        contexte.drawImage(imageLevier, transformation, null);
        contexte.dispose();
        return imageRetournee;
    }

    /* ____ ACTION : Initialise un levier inactif à sa position en pixels. ____ */
    public Levier(Map map, int idMecanisme, int positionX, int positionY) {
        super(map, positionX, positionY);
        this.idMecanisme = idMecanisme;
        int tailleTuile = map.getTailleTuile();
        this.hitbox = new int[]{tailleTuile, tailleTuile};
    }

    /* ____ ACTION : Retourne l'identifiant du mécanisme associé. ____ */
    public int getIdMecanisme() { return idMecanisme; }

    /* ____ ACTION : Indique si le levier est actif. ____ */
    public boolean isActive() { return estActive; }

    /* ____ ACTION : Définit l'état du levier. ____ */
    public void definirActif(boolean actif) { estActive = actif; }

    /* ____ ACTION : Bascule l'état actif du levier. ____ */
    public void basculer() { estActive = !estActive; }

    /* ____ ACTION : Indique si le joueur est en contact ou à portée du levier. ____ */
    public boolean estAProximite(Joueur joueur) {
        int[] hitboxJoueur = joueur.getHitbox();
        int marge = map.getTailleTuile();
        return joueur.getPositionX() - marge < positionX + hitbox[0]
                && joueur.getPositionX() + hitboxJoueur[0] + marge > positionX
                && joueur.getPositionY() - marge < positionY + hitbox[1]
                && joueur.getPositionY() + hitboxJoueur[1] + marge > positionY;
    }

    /* ____ ACTION : Retourne une hitbox de levier non solide. ____ */
    @Override public int[] getHitbox() { return hitbox; }
    /* ____ ACTION : Retourne la vitesse nulle du levier. ____ */
    @Override public int getVitesse() { return 0; }
    /* ____ ACTION : Retourne une impulsion nulle. ____ */
    @Override public int getForceSaut() { return 0; }
    /* ____ ACTION : Retourne un chemin d'image vide. ____ */
    @Override public String getCheminImage() { return ""; }
    /* ____ ACTION : Ne réalise aucun déplacement. ____ */
    @Override public void mettreAJour() { }

    /* ____ ACTION : Dessine le sprite entier du levier dans sa case.
     * [INFO] Le bas du sprite coïncide avec le bas de la case.
     * [DEPENDANCE] Le sprite basculé est le miroir horizontal du sprite normal. ____ */
    @Override public void dessiner(Graphics2D contexte, int x, int y) {
        BufferedImage imageLevier = estActive ? imageLevierRetournee : imageLevierNormale;
        if (imageLevier != null) {
            contexte.drawImage(imageLevier, x, y, hitbox[0], hitbox[1], null);
            return;
        }
        Color couleurPrecedente = contexte.getColor();
        contexte.setColor(estActive ? new Color(50, 220, 90) : new Color(220, 150, 40));
        contexte.fillRect(x, y, hitbox[0], hitbox[1]);
        contexte.setColor(couleurPrecedente);
    }
}
