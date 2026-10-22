// Map.java
package delaterrealassiette;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.imageio.ImageIO;

/* ________________________________________________________________
 * ACTION : Charge le décor depuis un fichier FruitEditor, l'affiche
 *          et détecte les collisions.
 * [INFO] decor[i][j] est le numéro de tuile ligne i (depuis le haut), colonne j.
 * [INFO] Pour les entités, l'origine est en bas à gauche et y monte.
 * [DEPENDANCE] Entite utilise collisionHitbox, Jeu appelle miseAJour et rendu.
 * ________________________________________________________________ */
public class Map {
    // Numéros des tuiles que les entités peuvent traverser (0 = ciel)
    private static final int[] TUILES_TRAVERSABLES = {0};

    private int largeur;
    private int hauteur;
    private int tailleTuile;

    // toutes les tuiles du tileset, numérotées ligne par ligne
    private BufferedImage[] tuiles;

    // decor[i][j] = numéro de la tuile à afficher à la ligne i, colonne j
    private int[][] decor;

    private boolean modifie;

    /* ________________________________________________________________
     * ACTION : Construit la map à partir d'un fichier texte FruitEditor.
     * [INFO] Format : nom de la carte, chemin du tileset,
     *        "largeur hauteur largeurTuile hauteurTuile",
     *        puis une ligne de numéros de tuiles par ligne du décor.
     * [INFO] Les chemins sont relatifs à la racine du projet NetBeans.
     * [INFO] Lecture séquentielle ligne par ligne, comme dans le poly.
     * ________________________________________________________________ */
    public Map(String nomFichier) {
        try {
            BufferedReader fichier = new BufferedReader(new FileReader(nomFichier));

            fichier.readLine(); // nom de la carte
            String cheminTileset = fichier.readLine();

            // "largeur hauteur largeurTuile hauteurTuile"
            String[] dimensions = fichier.readLine().split(" ");
            largeur = Integer.parseInt(dimensions[0]);
            hauteur = Integer.parseInt(dimensions[1]);
            tailleTuile = Integer.parseInt(dimensions[2]);

            // Une ligne du fichier = une ligne du décor
            decor = new int[hauteur][largeur];
            int i = 0;
            while (fichier.ready() && i < hauteur) {
                String ligne = fichier.readLine();
                String[] champs = ligne.split(" ");
                for (int j = 0; j < largeur; j++) {
                    decor[i][j] = Integer.parseInt(champs[j]);
                }
                i++;
            }

            fichier.close();

            BufferedImage tileset = ImageIO.read(new File(cheminTileset));
            decouperTileset(tileset);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ________________________________________________________________
     * ACTION : Retourne la largeur en nombre de tuiles.
     * ________________________________________________________________ */
    public int getLargeur() {
        return largeur;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hauteur en nombre de tuiles.
     * ________________________________________________________________ */
    public int getHauteur() {
        return hauteur;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la taille d'une tuile en pixels.
     * ________________________________________________________________ */
    public int getTailleTuile() {
        return tailleTuile;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la largeur du décor en pixels.
     * ________________________________________________________________ */
    public int getLargeurPixels() {
        return largeur * tailleTuile;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hauteur du décor en pixels.
     * ________________________________________________________________ */
    public int getHauteurPixels() {
        return hauteur * tailleTuile;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le numéro de tuile ligne i, colonne j.
     * ________________________________________________________________ */
    public int getTuile(int i, int j) {
        return decor[i][j];
    }

    /* ________________________________________________________________
     * ACTION : Change le numéro de tuile ligne i, colonne j.
     * [INFO] Une case hors du décor est ignorée.
     * [DEPENDANCE] GestionBDD exporte la map quand elle est modifiée.
     * ________________________________________________________________ */
    public void setTuile(int i, int j, int numero) {
        if (i < 0 || i >= hauteur || j < 0 || j >= largeur || decor[i][j] == numero) {
            return;
        }
        decor[i][j] = numero;
        modifie = true;
    }

    /* ________________________________________________________________
     * ACTION : Indique si le décor a changé depuis le dernier export.
     * ________________________________________________________________ */
    public boolean estModifie() {
        return modifie;
    }

    /* ________________________________________________________________
     * ACTION : Remet à zéro l'indicateur de modification.
     * ________________________________________________________________ */
    public void effacerModifie() {
        modifie = false;
    }

    /* ________________________________________________________________
     * ACTION : Découpe le tileset en tuiles numérotées ligne par ligne.
     * ________________________________________________________________ */
    private void decouperTileset(BufferedImage tileset) {
        int nbColonnes = tileset.getWidth() / tailleTuile;
        int nbLignes = tileset.getHeight() / tailleTuile;
        tuiles = new BufferedImage[nbColonnes * nbLignes];
        for (int i = 0; i < tuiles.length; i++) {
            int x = (i % nbColonnes) * tailleTuile;
            int y = (i / nbColonnes) * tailleTuile;
            tuiles[i] = tileset.getSubimage(x, y, tailleTuile, tailleTuile);
        }
    }

    /* ________________________________________________________________
     * ACTION : Actualise la map à chaque tour de boucle.
     * [INFO] Vide pour l'instant, le décor est statique.
     * ________________________________________________________________ */
    public void miseAJour() {

    }

    /* ________________________________________________________________
     * ACTION : Dessine le décor tuile par tuile.
     * ________________________________________________________________ */
    public void rendu(Graphics2D contexte) {
        // i parcourt les lignes, j parcourt les colonnes
        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {
                contexte.drawImage(tuiles[decor[i][j]], j * tailleTuile, i * tailleTuile, null);
            }
        }
    }

    /* ________________________________________________________________
     * ACTION : Indique si la case ligne i, colonne j bloque les entités.
     * [INFO] Hors du décor, les côtés sont solides, le haut et le bas non.
     * ________________________________________________________________ */
    public boolean estSolide(int i, int j) {
        if (j < 0 || j >= largeur) {
            return true;
        }
        if (i < 0 || i >= hauteur) {
            return false;
        }
        //On verifie si le numero de la tuile est parmi celles traversables
        for (int numero : TUILES_TRAVERSABLES) {
            if (decor[i][j] == numero) {
                return false;
            }
        }
        return true;
    }

    /* ________________________________________________________________
     * ACTION : Indique si une hitbox placée en (x, y) chevauche une tuile solide.
     * [INFO] L'origine de la hitbox est en bas à gauche et y monte.
     * [INFO] On suppose que y = 0 correspond au bas du décor.
     * ________________________________________________________________ */
    public boolean collisionHitbox(int x, int y, int largeurHitbox, int hauteurHitbox) {
        // Bords de la hitbox en pixels, -1 car le pixel numéro 0 existe
        int Hitbox_Pixel_Gauche = x;
        int Hitbox_Pixel_Droite = x + largeurHitbox - 1;
        int Hitbox_Pixel_Bas = y;
        int Hitbox_Pixel_Haut = y + hauteurHitbox - 1;

        // On prends le complementaire parce que la position d'un joueur est prise du sol alors que le decor du haut
        int Hitbox_Pixel_Haut_Decor = getHauteurPixels() - 1 - Hitbox_Pixel_Haut;
        int Hitbox_Pixel_Bas_Decor = getHauteurPixels() - 1 - Hitbox_Pixel_Bas;

        // Pixels -> tuiles, floorDiv car en Java -1 / 32 vaut 0 et pas -1
        int Hitbox_Tuile_Gauche = Math.floorDiv(Hitbox_Pixel_Gauche, tailleTuile);
        int Hitbox_Tuile_Droite = Math.floorDiv(Hitbox_Pixel_Droite, tailleTuile);
        int Hitbox_Tuile_Haut = Math.floorDiv(Hitbox_Pixel_Haut_Decor, tailleTuile);
        int Hitbox_Tuile_Bas = Math.floorDiv(Hitbox_Pixel_Bas_Decor, tailleTuile);

        // i parcourt les lignes du haut vers le bas, j les colonnes de gauche à droite
        for (int i = Hitbox_Tuile_Haut; i <= Hitbox_Tuile_Bas; i++) {
            for (int j = Hitbox_Tuile_Gauche; j <= Hitbox_Tuile_Droite; j++) {
                if (estSolide(i, j)) {
                    return true;
                }
            }
        }

        // Aucune tuile solide sous la hitbox, donc pas de collision
        return false;
    }
}
