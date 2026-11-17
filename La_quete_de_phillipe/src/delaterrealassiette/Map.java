// Map.java
package delaterrealassiette;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final List<ObjetInteractif> objetsDecor = new ArrayList<>();
    private final List<PositionMonstre> positionsMonstres = new ArrayList<>();
    private int[] positionDepart;

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
            while (i < hauteur) {
                String ligne = fichier.readLine();
                String[] champs = ligne.trim().split("\\s+");
                for (int j = 0; j < largeur; j++) {
                    decor[i][j] = Integer.parseInt(champs[j]);
                }
                i++;
            }
            String ligne;
            String section = "";
            int prochainIdentifiantPorte = 0;
            while ((ligne = fichier.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) {
                    continue;
                }
                String sectionCandidate = ligne.toUpperCase(java.util.Locale.ROOT);
                if ("SPAWN".equals(sectionCandidate)
                        || "INGREDIENTS".equals(sectionCandidate)
                        || "MECANISMES".equals(sectionCandidate)
                        || "MONSTRES".equals(sectionCandidate)) {
                    section = sectionCandidate;
                    continue;
                }
                String[] champs = ligne.split("\\s+");
                if ("SPAWN".equals(section)) {
                    int colonne = Integer.parseInt(champs[0]);
                    int ligneGrille = Integer.parseInt(champs[1]);
                    positionDepart = new int[]{colonne * tailleTuile,
                            getHauteurPixels() - (ligneGrille + 1) * tailleTuile};
                } else if ("INGREDIENTS".equals(section)) {
                    TypeIngredient type = TypeIngredient.valueOf(champs[0]);
                    int colonne = Integer.parseInt(champs[1]);
                    int ligneGrille = Integer.parseInt(champs[2]);
                    int tailleElement = Ingredient.TAILLE_HITBOX;
                    int positionX = colonne * tailleTuile + (tailleTuile - tailleElement) / 2;
                    int positionY = getHauteurPixels() - (ligneGrille + 1) * tailleTuile
                            + (tailleTuile - tailleElement) / 2;
                    ingredients.add(new Ingredient(type, positionX, positionY));
                } else if ("MONSTRES".equals(section)) {
                    String type = champs[0].toUpperCase(java.util.Locale.ROOT);
                    int colonne = Integer.parseInt(champs[1]);
                    int ligneGrille = Integer.parseInt(champs[2]);
                    int positionX = colonne * tailleTuile;
                    int positionY = getHauteurPixels() - ligneGrille * tailleTuile;
                    positionsMonstres.add(new PositionMonstre(type, positionX, positionY));
                } else if ("MECANISMES".equals(section)) {
                    if ("MORTEL".equalsIgnoreCase(champs[0])) {
                        int colonne = Integer.parseInt(champs[1]);
                        int ligneGrille = Integer.parseInt(champs[2]);
                        int largeurMortelle = champs.length > 3
                                ? Integer.parseInt(champs[3]) : 1;
                        int hauteurMortelle = champs.length > 4
                                ? Integer.parseInt(champs[4]) : 1;
                        int positionX = colonne * tailleTuile;
                        int positionY = getHauteurPixels()
                                - (ligneGrille + hauteurMortelle) * tailleTuile;
                        objetsDecor.add(new BlocMortel(this, positionX, positionY,
                                largeurMortelle * tailleTuile, hauteurMortelle * tailleTuile));
                    } else {
                        int identifiant = Integer.parseInt(champs[1]);
                        int colonne = Integer.parseInt(champs[2]);
                        int ligneGrille = Integer.parseInt(champs[3]);
                        int positionX = colonne * tailleTuile;
                        int positionY = getHauteurPixels() - (ligneGrille + 1) * tailleTuile;
                        if ("LEVIER".equalsIgnoreCase(champs[0])) {
                            objetsDecor.add(new Levier(this, identifiant, positionX, positionY));
                        } else if ("PORTE".equalsIgnoreCase(champs[0])) {
                            int largeurPorte = Integer.parseInt(champs[4]) * tailleTuile;
                            int hauteurPorte = Integer.parseInt(champs[5]) * tailleTuile;
                            boolean fermee = "FERMEE".equalsIgnoreCase(champs[6]);
                            objetsDecor.add(new BlocPorte(this, identifiant,
                                    prochainIdentifiantPorte++, positionX, positionY,
                                    largeurPorte, hauteurPorte, fermee));
                        }
                    }
                }
            }
            fichier.close();

            if (new File(cheminTileset).isFile()) {
                BufferedImage tileset = ImageIO.read(new File(cheminTileset));
                if (tileset != null) {
                    decouperTileset(tileset);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /* ________________________________________________________________
     * ACTION : Retourne la position de départ en pixels, depuis le bas-gauche.
     * [INFO] La position vaut null si le niveau ne déclare pas SPAWN.
     * ________________________________________________________________ */
    public int[] getPositionDepart() {
        return positionDepart == null ? null : positionDepart.clone();
    }

    /* ________________________________________________________________
     * ACTION : Retourne les types et positions des monstres déclarés.
     * ________________________________________________________________ */
    public List<PositionMonstre> getPositionsMonstres() {
        return Collections.unmodifiableList(positionsMonstres);
    }

    /* ________________________________________________________________
     * ACTION : Conserve le type et la position en pixels d'un monstre.
     * ________________________________________________________________ */
    public static final class PositionMonstre {
        private final String type;
        private final int positionX;
        private final int positionY;

        private PositionMonstre(String type, int positionX, int positionY) {
            this.type = type;
            this.positionX = positionX;
            this.positionY = positionY;
        }

        /* ________________________________________________________________
         * ACTION : Retourne le type de monstre déclaré.
         * ________________________________________________________________ */
        public String getType() {
            return type;
        }

        /* ________________________________________________________________
         * ACTION : Retourne la coordonnée horizontale en pixels.
         * ________________________________________________________________ */
        public int getPositionX() {
            return positionX;
        }

        /* ________________________________________________________________
         * ACTION : Retourne la coordonnée verticale en pixels.
         * ________________________________________________________________ */
        public int getPositionY() {
            return positionY;
        }
    }

    /* ________________________________________________________________
     * ACTION : Retourne les leviers définis dans le niveau.
     * ________________________________________________________________ */
    public List<Levier> getLeviers() {
        List<Levier> leviers = new ArrayList<>();
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Levier) {
                leviers.add((Levier) objet);
            }
        }
        return Collections.unmodifiableList(leviers);
    }

    /* ________________________________________________________________
     * ACTION : Retourne les blocs de porte définis dans le niveau.
     * ________________________________________________________________ */
    public List<BlocPorte> getBlocsPorte() {
        List<BlocPorte> blocsPorte = new ArrayList<>();
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof BlocPorte) {
                blocsPorte.add((BlocPorte) objet);
            }
        }
        return Collections.unmodifiableList(blocsPorte);
    }

    /* ________________________________________________________________
     * ACTION : Retourne les blocs mortels définis dans le niveau.
     * ________________________________________________________________ */
    public List<BlocMortel> getBlocsMortels() {
        List<BlocMortel> blocsMortels = new ArrayList<>();
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof BlocMortel) {
                blocsMortels.add((BlocMortel) objet);
            }
        }
        return Collections.unmodifiableList(blocsMortels);
    }

    /* ________________________________________________________________
     * ACTION : Retourne les objets du décor interactif hors ingrédients.
     * ________________________________________________________________ */
    public List<ObjetInteractif> getObjetsDecor() {
        return Collections.unmodifiableList(objetsDecor);
    }

    /* ________________________________________________________________
     * ACTION : Met à jour chaque porte selon le levier et son état initial.
     * [INFO] Seule l’occupation de la même porte retarde sa fermeture.
     * ________________________________________________________________ */
    public void mettreAJourMecanismes(List<Entite> entites) {
        List<BlocPorte> blocsPorte = getBlocsPorte();
        for (BlocPorte bloc : blocsPorte) {
            boolean actif = false;
            for (ObjetInteractif objet : objetsDecor) {
                if (!(objet instanceof Levier)) {
                    continue;
                }
                Levier levier = (Levier) objet;
                if (levier.getIdMecanisme() == bloc.getIdMecanisme()) {
                    actif = levier.isActive();
                    break;
                }
            }
            bloc.definirFermetureDemandee(bloc.isFermeeInitialement() != actif);
        }
        for (BlocPorte bloc : blocsPorte) {
            if (!bloc.estFermetureDemandee()) {
                bloc.definirFerme(false);
                continue;
            }
            boolean porteOccupee = false;
            for (BlocPorte candidat : blocsPorte) {
                if (candidat.getIdPorte() != bloc.getIdPorte()) {
                    continue;
                }
                for (Entite entite : entites) {
                    if ((entite instanceof Joueur || entite instanceof Monstre)
                            && candidat.enCollisionAvec(entite)) {
                        porteOccupee = true;
                        break;
                    }
                }
                if (porteOccupee) {
                    break;
                }
            }
            for (BlocPorte candidat : blocsPorte) {
                if (candidat.getIdPorte() == bloc.getIdPorte()) {
                    candidat.definirFerme(!porteOccupee);
                }
            }
        }
    }

    /* ________________________________________________________________
     * ACTION : Dessine la tuile demandée du tileset.
     * ________________________________________________________________ */
    public void dessinerTuile(Graphics2D contexte, int numero, int x, int y, int largeurTuile, int hauteurTuile) {
        if (tuiles != null && numero >= 0 && numero < tuiles.length) {
            contexte.drawImage(tuiles[numero], x, y, largeurTuile, hauteurTuile, null);
        }
    }

    /* ________________________________________________________________
     * ACTION : Retourne les ingrédients déclarés à la suite de la grille.
     * ________________________________________________________________ */
    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
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
    /* ________________________________________________________________
     * ACTION : Actualise la map à chaque tour de boucle.
     * [INFO] Le décor reste statique.
     * ________________________________________________________________ */
    public void miseAJour() {

    }

    /* ________________________________________________________________
     * ACTION : Dessine le décor tuile par tuile.
     * ________________________________________________________________ */
    public void rendu(Graphics2D contexte) {
        // i parcourt les lignes, j parcourt les colonnes
        if (tuiles == null) {
            return;
        }
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
        int hitboxPixelGauche = x;
        int hitboxPixelDroite = x + largeurHitbox - 1;
        int hitboxPixelBas = y;
        int hitboxPixelHaut = y + hauteurHitbox - 1;

        // On prends le complementaire parce que la position d'un joueur est prise du sol alors que le decor du haut
        int hitboxPixelHautDecor = getHauteurPixels() - 1 - hitboxPixelHaut;
        int hitboxPixelBasDecor = getHauteurPixels() - 1 - hitboxPixelBas;

        // Pixels -> tuiles, floorDiv car en Java -1 / 32 vaut 0 et pas -1
        int hitboxTuileGauche = Math.floorDiv(hitboxPixelGauche, tailleTuile);
        int hitboxTuileDroite = Math.floorDiv(hitboxPixelDroite, tailleTuile);
        int hitboxTuileHaut = Math.floorDiv(hitboxPixelHautDecor, tailleTuile);
        int hitboxTuileBas = Math.floorDiv(hitboxPixelBasDecor, tailleTuile);

        // i parcourt les lignes du haut vers le bas, j les colonnes de gauche à droite
        for (int i = hitboxTuileHaut; i <= hitboxTuileBas; i++) {
            for (int j = hitboxTuileGauche; j <= hitboxTuileDroite; j++) {
                if (estSolide(i, j)) {
                    return true;
                }
            }
        }

        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof BlocPorte && objet.isCollisionJoueur()
                    && objet.chevaucheRectangle(x, y, largeurHitbox, hauteurHitbox)) {
                return true;
            }
        }
        return false;
    }
}
