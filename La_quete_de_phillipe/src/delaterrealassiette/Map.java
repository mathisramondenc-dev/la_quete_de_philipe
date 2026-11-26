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

/**
 * Carte d'un niveau : décor en tuiles, objets posés dessus et règles de
 * collision.
 *
 * 1) Chargement : le constructeur lit un fichier texte niveauN.txt.
 *    - ligne 1 : nom du niveau (ignoré) ;
 *    - ligne 2 : chemin du tileset ;
 *    - ligne 3 : "largeur hauteur tailleTuile tailleTuile" (en tuiles / pixels) ;
 *    - puis une ligne de numéros de tuiles par ligne du décor (0 = ciel) ;
 *    - enfin des sections facultatives, un objet par ligne :
 *      SPAWN (colonne ligne), INGREDIENTS (TYPE colonne ligne),
 *      MONSTRES (TYPE colonne ligne) et MECANISMES (MORTEL, ECHELLE, CAISSE,
 *      LEVIER id..., PORTE id... FERMEE|OUVERTE).
 *    Les coordonnées du fichier sont en tuiles, ligne comptée depuis le haut.
 *
 * 2) Deux repères coexistent :
 *    - decor[i][j] : ligne i depuis le HAUT, colonne j (comme le fichier) ;
 *    - entités et objets : pixels, origine en BAS à gauche, y vers le haut.
 *    La conversion du bas d'une case est y = hauteurPixels - (ligne + 1) * tailleTuile.
 *
 * 3) Collisions : collisionHitbox() est le point d'entrée unique utilisé par
 *    toutes les entités. Est solide : toute tuile autre que le ciel, les
 *    bords gauche/droit de la carte, les portes fermées et les caisses. Le
 *    haut et le bas de la carte sont ouverts (on peut sauter et tomber).
 *
 * 4) Mécanismes : à chaque tick, mettreAJourMecanismes() lit l'état des
 *    leviers et ouvre/ferme les portes correspondantes.
 *
 * La Map est créée une seule fois par niveau (par Jeu) et partagée par
 * toutes les entités.
 */
public class Map {
    /** Numéros de tuiles que l'on traverse ; toutes les autres sont solides. */
    private static final int[] TUILES_TRAVERSABLES = {0};
    /** Tuile ciel : non dessinée quand une image de fond existe. */
    private static final int TUILE_CIEL = 0;
    private static final String CHEMIN_FOND = "images/fond_ville.png";

    private int largeur;
    private int hauteur;
    private int tailleTuile;

    /** Tuiles découpées dans le tileset, numérotées ligne par ligne. */
    private BufferedImage[] tuiles;
    /** Image de fond facultative (null si absente). */
    private BufferedImage fond;
    /** decor[i][j] = numéro de tuile, ligne i depuis le haut, colonne j. */
    private int[][] decor;

    private boolean modifie;
    private final List<Ingredient> ingredients = new ArrayList<>();
    /** Leviers, portes, échelles et blocs mortels (objets fixes). */
    private final List<ObjetInteractif> objetsDecor = new ArrayList<>();
    /** Les caisses sont à part car elles bougent et bloquent les entités. */
    private final List<Caisse> caisses = new ArrayList<>();
    private final List<Monstre> monstres = new ArrayList<>();
    private int[] positionDepart;

    /**
     * Lit le fichier de niveau (format décrit en tête de classe), crée les
     * objets déclarés puis charge les images. En cas d'erreur de lecture, la
     * trace est affichée et la carte reste incomplète.
     *
     * @param nomFichier chemin du fichier, relatif à la racine du projet
     */
    public Map(String nomFichier) {
        try {
            BufferedReader fichier = new BufferedReader(new FileReader(nomFichier));

            // ===== En-tête =====
            fichier.readLine(); // nom de la carte, non utilisé
            String cheminTileset = fichier.readLine();
            String[] dimensions = fichier.readLine().split(" ");
            largeur = Integer.parseInt(dimensions[0]);
            hauteur = Integer.parseInt(dimensions[1]);
            tailleTuile = Integer.parseInt(dimensions[2]);

            // ===== Grille du décor =====
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

            // ===== Sections d'objets =====
            // On lit jusqu'à la fin en retenant la section en cours ; chaque
            // ligne de données est convertie en objet, positionné en pixels.
            String ligne;
            String section = "";
            // Numéro propre à chaque ligne PORTE, pour regrouper ses blocs
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
                    // Plus petit qu'une tuile : centré dans sa case
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
                    // La case indiquée est le sol : le monstre est posé dessus
                    int positionY = getHauteurPixels() - ligneGrille * tailleTuile;
                    // Seul le rat existe ; un type inconnu est ignoré
                    if ("RAT".equals(type)) {
                        monstres.add(Monstre.creerRat(this, positionX, positionY));
                    }

                } else if ("MECANISMES".equals(section)) {
                    if ("MORTEL".equalsIgnoreCase(champs[0])) {
                        // MORTEL colonne ligne [largeur] [hauteur] : taille 1x1 par défaut
                        int colonne = Integer.parseInt(champs[1]);
                        int ligneGrille = Integer.parseInt(champs[2]);

                        int largeurMortelle;
                        if (champs.length > 3) {
                            largeurMortelle = Integer.parseInt(champs[3]);
                        } else {
                            largeurMortelle = 1;
                        }

                        int hauteurMortelle;
                        if (champs.length > 4) {
                            hauteurMortelle = Integer.parseInt(champs[4]);
                        } else {
                            hauteurMortelle = 1;
                        }

                        int positionX = colonne * tailleTuile;
                        // ligneGrille désigne le haut de la zone
                        int positionY = getHauteurPixels()
                                - (ligneGrille + hauteurMortelle) * tailleTuile;
                        objetsDecor.add(new BlocMortel(this, positionX, positionY,
                                largeurMortelle * tailleTuile, hauteurMortelle * tailleTuile));

                    } else if ("ECHELLE".equalsIgnoreCase(champs[0])) {
                        // ECHELLE colonne ligne largeur hauteur (ligne = haut de l'échelle)
                        int colonne = Integer.parseInt(champs[1]);
                        int ligneGrille = Integer.parseInt(champs[2]);
                        int largeurEchelle = Integer.parseInt(champs[3]);
                        int hauteurEchelle = Integer.parseInt(champs[4]);
                        int positionX = colonne * tailleTuile;
                        int positionY = getHauteurPixels() - (ligneGrille + hauteurEchelle) * tailleTuile;
                        objetsDecor.add(new Echelle(this, positionX, positionY,
                                largeurEchelle * tailleTuile, hauteurEchelle * tailleTuile));

                    } else if ("CAISSE".equalsIgnoreCase(champs[0])) {
                        // CAISSE colonne ligne
                        int colonne = Integer.parseInt(champs[1]);
                        int ligneGrille = Integer.parseInt(champs[2]);
                        int positionX = colonne * tailleTuile;
                        int positionY = getHauteurPixels() - (ligneGrille + 1) * tailleTuile;
                        caisses.add(new Caisse(this, positionX, positionY));

                    } else {
                        // LEVIER id colonne ligne
                        // PORTE id colonne ligne largeur hauteur FERMEE|OUVERTE
                        // Un levier commande toutes les portes de même id.
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

            // ===== Images (facultatives : le jeu tourne sans) =====
            if (new File(cheminTileset).isFile()) {
                BufferedImage tileset = ImageIO.read(new File(cheminTileset));
                if (tileset != null) {
                    decouperTileset(tileset);
                }
            }

            if (new File(CHEMIN_FOND).isFile()) {
                fond = ImageIO.read(new File(CHEMIN_FOND));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // ----- Contenu du niveau (listes en lecture seule) -----

    /**
     * @return copie de la position {x, y} de départ en pixels,
     *         ou null si le niveau n'a pas de section SPAWN
     */
    public int[] getPositionDepart() {
        if (positionDepart == null) {
            return null;
        }
        return positionDepart.clone();
    }

    public List<Monstre> getMonstres() {
        return Collections.unmodifiableList(monstres);
    }

    /** @return les leviers, extraits de la liste des objets du décor */
    public List<Levier> getLeviers() {
        List<Levier> leviers = new ArrayList<>();
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Levier) {
                leviers.add((Levier) objet);
            }
        }
        return Collections.unmodifiableList(leviers);
    }

    /** @return les blocs de porte, extraits de la liste des objets du décor */
    public List<BlocPorte> getBlocsPorte() {
        List<BlocPorte> blocsPorte = new ArrayList<>();
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof BlocPorte) {
                blocsPorte.add((BlocPorte) objet);
            }
        }
        return Collections.unmodifiableList(blocsPorte);
    }

    /** @return les blocs mortels, extraits de la liste des objets du décor */
    public List<BlocMortel> getBlocsMortels() {
        List<BlocMortel> blocsMortels = new ArrayList<>();
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof BlocMortel) {
                blocsMortels.add((BlocMortel) objet);
            }
        }
        return Collections.unmodifiableList(blocsMortels);
    }

    public List<Caisse> getCaisses() {
        return Collections.unmodifiableList(caisses);
    }

    // ----- Sol et échelles -----

    /**
     * @return true si la hitbox, décalée d'un pixel vers le bas, touche un
     *         élément solide ou repose exactement sur le sommet d'une échelle
     */
    public boolean aUnSolSous(int x, int y, int largeurHitbox, int hauteurHitbox) {
        if (collisionHitbox(x, y - 1, largeurHitbox, hauteurHitbox)) {
            return true;
        }
        return estSurSommetEchelle(x, y, largeurHitbox);
    }

    /** @return true si les pieds (y) sont pile au sommet d'une échelle alignée horizontalement */
    public boolean estSurSommetEchelle(int x, int y, int largeurHitbox) {
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Echelle) {
                int haut = objet.getPositionY() + objet.getHitbox()[1];
                if (y == haut && x < objet.getPositionX() + objet.getHitbox()[0]
                        && x + largeurHitbox > objet.getPositionX()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Détecte une entité qui traverse le sommet d'une échelle en tombant,
     * pour qu'Entite puisse l'arrêter dessus.
     *
     * @return hauteur du sommet franchi, ou Integer.MIN_VALUE si aucun
     */
    public int getSommetEchelleFranchi(int x, int anciennePositionY, int nouvellePositionY,
            int largeurHitbox) {
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Echelle) {
                int haut = objet.getPositionY() + objet.getHitbox()[1];
                if (anciennePositionY >= haut && nouvellePositionY < haut
                        && x < objet.getPositionX() + objet.getHitbox()[0]
                        && x + largeurHitbox > objet.getPositionX()) {
                    return haut;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    /** @return true si la hitbox est dans une échelle ou posée sur son sommet */
    public boolean estSurOuDansEchelle(int x, int y, int largeurHitbox,
            int hauteurHitbox) {
        if (chevaucheEchelle(x, y, largeurHitbox, hauteurHitbox)) {
            return true;
        }
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Echelle) {
                int haut = objet.getPositionY() + objet.getHitbox()[1];
                if (y == haut && x < objet.getPositionX() + objet.getHitbox()[0]
                        && x + largeurHitbox > objet.getPositionX()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean chevaucheEchelle(int x, int y, int largeurHitbox, int hauteurHitbox) {
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Echelle
                    && objet.chevaucheRectangle(x, y, largeurHitbox, hauteurHitbox)) {
                return true;
            }
        }
        return false;
    }

    // ----- Caisses -----

    /**
     * Pousse une caisse d'un pixel. Les caisses collées devant sont poussées
     * d'abord, récursivement : si la dernière est bloquée, toute la rangée
     * l'est.
     *
     * @param direction -1 vers la gauche, 1 vers la droite
     * @return true si la caisse a avancé
     */
    public boolean pousserCaisse(Caisse caisse, int direction) {
        int prochainePositionX = caisse.getPositionX() + direction;

        for (Caisse autre : caisses) {
            if (autre != caisse && autre.chevaucheRectangle(prochainePositionX,
                    caisse.getPositionY(), caisse.getHitbox()[0], caisse.getHitbox()[1])) {
                if (!pousserCaisse(autre, direction)) {
                    return false;
                }
            }
        }

        if (collisionHitboxSansCaisses(prochainePositionX, caisse.getPositionY(),
                caisse.getHitbox()[0], caisse.getHitbox()[1], caisse)) {
            return false;
        }

        caisse.setPositionX(prochainePositionX);
        return true;
    }

    /**
     * Comme collisionHitbox(), mais en ignorant une caisse : utilisé par les
     * caisses pour ne pas se bloquer elles-mêmes.
     */
    public boolean collisionHitboxSansCaisses(int x, int y, int largeurHitbox,
            int hauteurHitbox, Caisse caisseIgnoree) {
        if (collisionTerrainEtPortes(x, y, largeurHitbox, hauteurHitbox)) {
            return true;
        }
        for (Caisse caisse : caisses) {
            if (caisse != caisseIgnoree
                    && caisse.chevaucheRectangle(x, y, largeurHitbox, hauteurHitbox)) {
                return true;
            }
        }
        return false;
    }

    /** @return leviers, portes, échelles et blocs mortels (sans ingrédients ni caisses) */
    public List<ObjetInteractif> getObjetsDecor() {
        return Collections.unmodifiableList(objetsDecor);
    }

    // ----- Leviers et portes -----

    /**
     * Met les portes dans l'état voulu par leurs leviers, en deux étapes :
     * 1) chaque porte calcule l'état demandé : fermée si son état initial
     *    et l'état du levier diffèrent (le levier inverse l'état de départ) ;
     * 2) l'ouverture est immédiate ; la fermeture n'a lieu que si aucune
     *    entité n'occupe l'un des blocs de la même porte (même idPorte),
     *    sinon on réessaie au tick suivant.
     *
     * @param entites joueurs, monstres et caisses du niveau
     */
    public void mettreAJourMecanismes(List<Entite> entites) {
        List<BlocPorte> blocsPorte = getBlocsPorte();

        // Étape 1 : état demandé par le levier
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

        // Étape 2 : application, sans enfermer personne
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
                    if (candidat.enCollisionAvec(entite)) {
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

    // ----- Tuiles et dimensions -----

    /** Dessine une tuile du tileset à l'écran ; ne fait rien si le numéro n'existe pas. */
    public void dessinerTuile(Graphics2D contexte, int numero, int x, int y, int largeurTuile, int hauteurTuile) {
        if (tuiles != null && numero >= 0 && numero < tuiles.length) {
            contexte.drawImage(tuiles[numero], x, y, largeurTuile, hauteurTuile, null);
        }
    }

    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    /** @return largeur en nombre de tuiles */
    public int getLargeur() {
        return largeur;
    }

    /** @return hauteur en nombre de tuiles */
    public int getHauteur() {
        return hauteur;
    }

    /** @return côté d'une tuile en pixels */
    public int getTailleTuile() {
        return tailleTuile;
    }

    public int getLargeurPixels() {
        return largeur * tailleTuile;
    }

    public int getHauteurPixels() {
        return hauteur * tailleTuile;
    }

    /** @return numéro de la tuile ligne i (depuis le haut), colonne j */
    public int getTuile(int i, int j) {
        return decor[i][j];
    }

    /**
     * Change une tuile du décor et marque la carte comme modifiée pour que
     * GestionBDD la réexporte. Une case hors carte est ignorée.
     */
    public void setTuile(int i, int j, int numero) {
        if (i < 0 || i >= hauteur || j < 0 || j >= largeur || decor[i][j] == numero) {
            return;
        }
        decor[i][j] = numero;
        modifie = true;
    }

    /** @return true si une tuile a changé depuis le dernier export */
    public boolean estModifie() {
        return modifie;
    }

    /** À appeler après l'export de la carte. */
    public void effacerModifie() {
        modifie = false;
    }

    /** Découpe le tileset en carrés de tailleTuile, numérotés de gauche à droite puis ligne par ligne. */
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

    /** Appelée à chaque tick ; le décor est statique (les portes sont gérées par mettreAJourMecanismes). */
    public void miseAJour() {
    }

    /**
     * Dessine le fond puis les tuiles. Quand un fond existe, les cases de
     * ciel ne sont pas dessinées pour laisser voir la ville derrière.
     * Ici le repère est celui de l'écran (y vers le bas).
     */
    public void rendu(Graphics2D contexte) {
        if (fond != null) {
            contexte.drawImage(fond, 0, 0, getLargeurPixels(), getHauteurPixels(), null);
        }

        if (tuiles == null) {
            return;
        }

        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {
                if (fond != null && decor[i][j] == TUILE_CIEL) {
                    continue;
                }
                contexte.drawImage(tuiles[decor[i][j]], j * tailleTuile, i * tailleTuile, null);
            }
        }
    }

    // ----- Collisions -----

    /**
     * @return true si la case bloque. Hors carte : les côtés sont des murs
     *         invisibles, le haut et le bas sont vides.
     */
    public boolean estSolide(int i, int j) {
        if (j < 0 || j >= largeur) {
            return true;
        }
        if (i < 0 || i >= hauteur) {
            return false;
        }
        for (int numero : TUILES_TRAVERSABLES) {
            if (decor[i][j] == numero) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test de collision commun à toutes les entités : tuiles solides, portes
     * fermées et caisses.
     *
     * @param x abscisse du coin bas-gauche de la hitbox (pixels)
     * @param y ordonnée du coin bas-gauche, y = 0 en bas de la carte
     * @return true si la hitbox placée là touche un obstacle
     */
    public boolean collisionHitbox(int x, int y, int largeurHitbox, int hauteurHitbox) {
        if (collisionTerrainEtPortes(x, y, largeurHitbox, hauteurHitbox)) {
            return true;
        }
        for (Caisse caisse : caisses) {
            if (caisse.chevaucheRectangle(x, y, largeurHitbox, hauteurHitbox)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convertit la hitbox (pixels, y vers le haut) en plage de cases du
     * décor (y vers le bas), puis teste chaque case couverte ; teste ensuite
     * les portes fermées. floorDiv arrondit correctement les coordonnées
     * négatives quand l'entité sort de la carte.
     */
    private boolean collisionTerrainEtPortes(int x, int y, int largeurHitbox,
            int hauteurHitbox) {
        // Bords de la hitbox en pixels (le "- 1" : 32 px depuis 0 finissent au pixel 31)
        int hitboxPixelGauche = x;
        int hitboxPixelDroite = x + largeurHitbox - 1;
        int hitboxPixelBas = y;
        int hitboxPixelHaut = y + hauteurHitbox - 1;

        // Passage au repère du décor : le haut de la hitbox devient la plus petite ligne
        int hitboxPixelHautDecor = getHauteurPixels() - 1 - hitboxPixelHaut;
        int hitboxPixelBasDecor = getHauteurPixels() - 1 - hitboxPixelBas;

        int hitboxTuileGauche = Math.floorDiv(hitboxPixelGauche, tailleTuile);
        int hitboxTuileDroite = Math.floorDiv(hitboxPixelDroite, tailleTuile);
        int hitboxTuileHaut = Math.floorDiv(hitboxPixelHautDecor, tailleTuile);
        int hitboxTuileBas = Math.floorDiv(hitboxPixelBasDecor, tailleTuile);

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
