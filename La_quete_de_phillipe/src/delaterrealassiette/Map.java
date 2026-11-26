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

    // Numéro de la tuile ciel, remplacée par l'image de fond si elle existe
    private static final int TUILE_CIEL = 0;

    // Image de fond (ciel + ville) dessinée derrière les tuiles
    private static final String CHEMIN_FOND = "images/fond_ville.png";

    private int largeur;
    private int hauteur;
    private int tailleTuile;

    // toutes les tuiles du tileset, numérotées ligne par ligne
    private BufferedImage[] tuiles;

    // image de fond, null si le fichier est absent
    private BufferedImage fond;

    // decor[i][j] = numéro de la tuile à afficher à la ligne i, colonne j
    private int[][] decor;

    private boolean modifie;
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final List<ObjetInteractif> objetsDecor = new ArrayList<>();
    private final List<Caisse> caisses = new ArrayList<>();
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
        // try : la lecture d'un fichier peut échouer (fichier absent, illisible...).
        // Si c'est le cas, Java lance une IOException qu'on attrape dans le catch.
        try {
            // On ouvre le fichier du niveau. BufferedReader permet de le lire
            // ligne par ligne avec readLine() (renvoie null à la fin du fichier).
            BufferedReader fichier = new BufferedReader(new FileReader(nomFichier));

            // ===== 1) EN-TÊTE DU FICHIER (3 premières lignes) =====

            // Ligne 1 : nom de la carte (ex : "Niveau 1"), on ne s'en sert pas
            fichier.readLine(); // nom de la carte

            // Ligne 2 : chemin de l'image du tileset (ex : "images/tileSetVille32x32.png")
            String cheminTileset = fichier.readLine();

            // Ligne 3 : "largeur hauteur largeurTuile hauteurTuile" (ex : "60 33 32 32")
            // split(" ") découpe la ligne à chaque espace -> tableau de 4 textes
            String[] dimensions = fichier.readLine().split(" ");
            largeur = Integer.parseInt(dimensions[0]);     // nombre de colonnes de tuiles
            hauteur = Integer.parseInt(dimensions[1]);     // nombre de lignes de tuiles
            tailleTuile = Integer.parseInt(dimensions[2]); // taille d'une tuile en pixels (carrée)

            // ===== 2) GRILLE DU DÉCOR =====

            // Une ligne du fichier = une ligne du décor.
            // Chaque nombre est le numéro d'une tuile du tileset (0 = ciel, 2 = trottoir, 3 = briques...)
            decor = new int[hauteur][largeur];
            int i = 0;
            while (i < hauteur) {
                String ligne = fichier.readLine();
                // trim() enlève les espaces au début et à la fin,
                // split("\\s+") découpe sur un ou plusieurs espaces/tabulations
                String[] champs = ligne.trim().split("\\s+");
                for (int j = 0; j < largeur; j++) {
                    // On convertit le texte en entier et on le range dans la case (i, j)
                    decor[i][j] = Integer.parseInt(champs[j]);
                }
                i++;
            }

            // ===== 3) SECTIONS APRÈS LA GRILLE (SPAWN, INGREDIENTS, MECANISMES, MONSTRES) =====
            // Le fichier contient ensuite des titres de section, suivis de lignes de données.
            // On lit jusqu'à la fin du fichier en retenant dans quelle section on se trouve.

            String ligne;
            String section = ""; // section en cours de lecture ("" tant qu'aucun titre n'a été lu)

            // Chaque ligne PORTE reçoit son propre numéro (0, 1, 2...) pour la distinguer
            // des autres portes, même si elles sont reliées au même levier.
            int prochainIdentifiantPorte = 0;

            // On lit une ligne ; tant qu'elle n'est pas null, le fichier n'est pas fini
            while ((ligne = fichier.readLine()) != null) {
                ligne = ligne.trim();

                // Ligne vide : on l'ignore et on passe à la suivante
                if (ligne.isEmpty()) {
                    continue;
                }

                // Si la ligne est un titre de section, on change de section
                // (toUpperCase pour accepter "Spawn", "spawn", "SPAWN"...)
                String sectionCandidate = ligne.toUpperCase(java.util.Locale.ROOT);
                if ("SPAWN".equals(sectionCandidate)
                        || "INGREDIENTS".equals(sectionCandidate)
                        || "MECANISMES".equals(sectionCandidate)
                        || "MONSTRES".equals(sectionCandidate)) {
                    section = sectionCandidate;
                    continue;
                }

                // Sinon c'est une ligne de données : on la découpe en morceaux
                String[] champs = ligne.split("\\s+");

                // RAPPEL DES COORDONNÉES :
                // - dans le fichier, (colonne, ligneGrille) est compté en tuiles,
                //   ligneGrille partant du HAUT de la carte (comme decor[i][j]) ;
                // - les objets du jeu sont en pixels, avec y qui part du BAS et monte.
                // Pour convertir le bas d'une case : y = hauteurPixels - (ligneGrille + 1) * tailleTuile

                if ("SPAWN".equals(section)) {
                    // Format : "colonne ligne" -> position de départ du joueur
                    int colonne = Integer.parseInt(champs[0]);
                    int ligneGrille = Integer.parseInt(champs[1]);
                    positionDepart = new int[]{colonne * tailleTuile,
                            getHauteurPixels() - (ligneGrille + 1) * tailleTuile};

                } else if ("INGREDIENTS".equals(section)) {
                    // Format : "TYPE colonne ligne" (ex : "TOMATE 21 16")
                    // valueOf transforme le texte en valeur de l'énumération TypeIngredient
                    TypeIngredient type = TypeIngredient.valueOf(champs[0]);
                    int colonne = Integer.parseInt(champs[1]);
                    int ligneGrille = Integer.parseInt(champs[2]);
                    // L'ingrédient est plus petit qu'une tuile : on le centre dans sa case
                    int tailleElement = Ingredient.TAILLE_HITBOX;
                    int positionX = colonne * tailleTuile + (tailleTuile - tailleElement) / 2;
                    int positionY = getHauteurPixels() - (ligneGrille + 1) * tailleTuile
                            + (tailleTuile - tailleElement) / 2;
                    ingredients.add(new Ingredient(type, positionX, positionY));

                } else if ("MONSTRES".equals(section)) {
                    // Format : "TYPE colonne ligne" (ex : "RAT 20 17")
                    // On ne crée pas le monstre ici : on garde juste son type et sa position,
                    // c'est Jeu qui le crée ensuite avec getPositionsMonstres().
                    String type = champs[0].toUpperCase(java.util.Locale.ROOT);
                    int colonne = Integer.parseInt(champs[1]);
                    int ligneGrille = Integer.parseInt(champs[2]);
                    int positionX = colonne * tailleTuile;
                    // Pas de "+ 1" ici : les pieds du monstre sont posés sur le HAUT
                    // de la case indiquée (la case donnée est le sol sous le monstre)
                    int positionY = getHauteurPixels() - ligneGrille * tailleTuile;
                    positionsMonstres.add(new PositionMonstre(type, positionX, positionY));

                } else if ("MECANISMES".equals(section)) {
                    // Le premier mot indique le type de mécanisme

                    if ("MORTEL".equalsIgnoreCase(champs[0])) {
                        // Format : "MORTEL colonne ligne [largeur] [hauteur]" (en tuiles)
                        // La largeur et la hauteur sont facultatives : 1 par défaut
                        int colonne = Integer.parseInt(champs[1]);
                        int ligneGrille = Integer.parseInt(champs[2]);
                        int largeurMortelle = champs.length > 3
                                ? Integer.parseInt(champs[3]) : 1;
                        int hauteurMortelle = champs.length > 4
                                ? Integer.parseInt(champs[4]) : 1;
                        int positionX = colonne * tailleTuile;
                        // ligneGrille est la ligne du HAUT de la zone : le bas de la zone
                        // est donc hauteurMortelle tuiles plus bas
                        int positionY = getHauteurPixels()
                                - (ligneGrille + hauteurMortelle) * tailleTuile;
                        objetsDecor.add(new BlocMortel(this, positionX, positionY,
                                largeurMortelle * tailleTuile, hauteurMortelle * tailleTuile));

                    } else if ("ECHELLE".equalsIgnoreCase(champs[0])) {
                        // Format : "ECHELLE colonne ligne largeur hauteur" (en tuiles)
                        int colonne = Integer.parseInt(champs[1]);
                        int ligneGrille = Integer.parseInt(champs[2]);
                        int largeurEchelle = Integer.parseInt(champs[3]);
                        int hauteurEchelle = Integer.parseInt(champs[4]);
                        int positionX = colonne * tailleTuile;
                        // Même calcul que MORTEL : ligneGrille = haut de l'échelle
                        int positionY = getHauteurPixels() - (ligneGrille + hauteurEchelle) * tailleTuile;
                        objetsDecor.add(new Echelle(this, positionX, positionY,
                                largeurEchelle * tailleTuile, hauteurEchelle * tailleTuile));

                    } else if ("CAISSE".equalsIgnoreCase(champs[0])) {
                        // Format : "CAISSE colonne ligne" -> caisse d'une tuile
                        // Rangée à part (liste caisses) car elle bouge et bloque les entités
                        int colonne = Integer.parseInt(champs[1]);
                        int ligneGrille = Integer.parseInt(champs[2]);
                        int positionX = colonne * tailleTuile;
                        int positionY = getHauteurPixels() - (ligneGrille + 1) * tailleTuile;
                        caisses.add(new Caisse(this, positionX, positionY));

                    } else {
                        // LEVIER et PORTE commencent pareil : "TYPE identifiant colonne ligne ..."
                        // L'identifiant relie un levier aux portes qu'il commande
                        // (LEVIER 1 ... actionne toutes les PORTE 1 ...)
                        int identifiant = Integer.parseInt(champs[1]);
                        int colonne = Integer.parseInt(champs[2]);
                        int ligneGrille = Integer.parseInt(champs[3]);
                        int positionX = colonne * tailleTuile;
                        int positionY = getHauteurPixels() - (ligneGrille + 1) * tailleTuile;

                        if ("LEVIER".equalsIgnoreCase(champs[0])) {
                            // Format : "LEVIER identifiant colonne ligne"
                            objetsDecor.add(new Levier(this, identifiant, positionX, positionY));

                        } else if ("PORTE".equalsIgnoreCase(champs[0])) {
                            // Format : "PORTE identifiant colonne ligne largeur hauteur FERMEE|OUVERTE"
                            int largeurPorte = Integer.parseInt(champs[4]) * tailleTuile;
                            int hauteurPorte = Integer.parseInt(champs[5]) * tailleTuile;
                            // État de départ de la porte (avant que le levier soit actionné)
                            boolean fermee = "FERMEE".equalsIgnoreCase(champs[6]);
                            // prochainIdentifiantPorte++ : on donne le numéro actuel puis on l'augmente
                            objetsDecor.add(new BlocPorte(this, identifiant,
                                    prochainIdentifiantPorte++, positionX, positionY,
                                    largeurPorte, hauteurPorte, fermee));
                        }
                    }
                }
            }
            // Fin du fichier : on le ferme pour libérer la ressource
            fichier.close();

            // ===== 4) CHARGEMENT DES IMAGES =====

            // On charge le tileset seulement s'il existe, puis on le découpe en tuiles
            // (tuiles[0], tuiles[1]... numérotées ligne par ligne dans l'image)
            if (new File(cheminTileset).isFile()) {
                BufferedImage tileset = ImageIO.read(new File(cheminTileset));
                if (tileset != null) { // null si le fichier n'est pas une image valide
                    decouperTileset(tileset);
                }
            }

            // Image de fond (ciel + ville) : facultative, rendu() s'en passe si elle manque
            if (new File(CHEMIN_FOND).isFile()) {
                fond = ImageIO.read(new File(CHEMIN_FOND));
            }

        } catch (IOException e) {
            // En cas d'erreur de lecture, on affiche le détail de l'erreur dans la console
            // (la carte reste alors vide ou incomplète)
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
     * ACTION : Retourne les caisses présentes sur la carte.
     * ________________________________________________________________ */
    public List<Caisse> getCaisses() {
        return Collections.unmodifiableList(caisses);
    }

    /* ________________________________________________________________
     * ACTION : Indique si une hitbox peut se poser sur un sol ou une échelle.
     * ________________________________________________________________ */
    public boolean aUnSolSous(int x, int y, int largeurHitbox, int hauteurHitbox) {
        if (collisionHitbox(x, y - 1, largeurHitbox, hauteurHitbox)) {
            return true;
        }
        return estSurSommetEchelle(x, y, largeurHitbox);
    }

    /* ________________________________________________________________
     * ACTION : Détecte un contact exact avec le dessus d'une échelle.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Détecte le franchissement descendant du sommet d'une échelle.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Indique si une hitbox est sur ou dans une échelle.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Indique si une hitbox chevauche une échelle.
     * ________________________________________________________________ */
    public boolean chevaucheEchelle(int x, int y, int largeurHitbox, int hauteurHitbox) {
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Echelle
                    && objet.chevaucheRectangle(x, y, largeurHitbox, hauteurHitbox)) {
                return true;
            }
        }
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Essaie de pousser une caisse et les caisses qui la précèdent.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Met à jour les caisses sans inclure leur propre hitbox.
     * ________________________________________________________________ */
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
     * ACTION : Dessine l'image de fond puis le décor tuile par tuile.
     * [INFO] Avec un fond, les tuiles ciel ne sont pas dessinées
     *        pour laisser voir la ville derrière.
     * ________________________________________________________________ */
    public void rendu(Graphics2D contexte) {
        if (fond != null) {
            contexte.drawImage(fond, 0, 0, getLargeurPixels(), getHauteurPixels(), null);
        }
        // i parcourt les lignes, j parcourt les colonnes
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

    /* ________________________________________________________________
     * ACTION : Teste les tuiles solides et les portes fermées.
     * ________________________________________________________________ */
    private boolean collisionTerrainEtPortes(int x, int y, int largeurHitbox,
            int hauteurHitbox) {
        int hitboxPixelGauche = x;
        int hitboxPixelDroite = x + largeurHitbox - 1;
        int hitboxPixelBas = y;
        int hitboxPixelHaut = y + hauteurHitbox - 1;
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
