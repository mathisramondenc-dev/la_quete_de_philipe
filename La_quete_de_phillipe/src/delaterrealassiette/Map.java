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
    private final List<Monstre> monstres = new ArrayList<>();
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
                    // Comme les caisses, le monstre est créé directement ici
                    // et Jeu le récupère ensuite avec getMonstres().
                    String type = champs[0].toUpperCase(java.util.Locale.ROOT);
                    int colonne = Integer.parseInt(champs[1]);
                    int ligneGrille = Integer.parseInt(champs[2]);
                    int positionX = colonne * tailleTuile;
                    // Pas de "+ 1" ici : les pieds du monstre sont posés sur le HAUT
                    // de la case indiquée (la case donnée est le sol sous le monstre)
                    int positionY = getHauteurPixels() - ligneGrille * tailleTuile;
                    // Seul le rat existe pour l'instant : un type inconnu est ignoré
                    if ("RAT".equals(type)) {
                        monstres.add(Monstre.creerRat(this, positionX, positionY));
                    }

                } else if ("MECANISMES".equals(section)) {
                    // Le premier mot indique le type de mécanisme

                    if ("MORTEL".equalsIgnoreCase(champs[0])) {
                        // Format : "MORTEL colonne ligne [largeur] [hauteur]" (en tuiles)
                        // La largeur et la hauteur sont facultatives : 1 par défaut
                        int colonne = Integer.parseInt(champs[1]);
                        int ligneGrille = Integer.parseInt(champs[2]);

                        // Largeur : 4e mot de la ligne s'il existe, sinon 1 tuile
                        // (on teste la longueur pour ne pas lire une case qui n'existe pas)
                        int largeurMortelle;
                        if (champs.length > 3) {
                            largeurMortelle = Integer.parseInt(champs[3]);
                        } else {
                            largeurMortelle = 1;
                        }

                        // Hauteur : 5e mot de la ligne s'il existe, sinon 1 tuile
                        int hauteurMortelle;
                        if (champs.length > 4) {
                            hauteurMortelle = Integer.parseInt(champs[4]);
                        } else {
                            hauteurMortelle = 1;
                        }

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
        // Pas de section SPAWN dans le fichier : on renvoie null,
        // Jeu utilisera alors sa position de départ par défaut
        if (positionDepart == null) {
            return null;
        }
        // clone() renvoie une COPIE du tableau {x, y} : si l'appelant la modifie,
        // la position de départ enregistrée dans la map ne change pas
        return positionDepart.clone();
    }

    /* ________________________________________________________________
     * ACTION : Retourne les monstres déclarés dans le niveau.
     * ________________________________________________________________ */
    public List<Monstre> getMonstres() {
        // unmodifiableList : l'appelant peut lire la liste mais pas y ajouter
        // ou retirer de monstre (seule la map remplit cette liste)
        return Collections.unmodifiableList(monstres);
    }

    /* ________________________________________________________________
     * ACTION : Retourne les leviers définis dans le niveau.
     * ________________________________________________________________ */
    public List<Levier> getLeviers() {
        // Les leviers sont mélangés aux autres objets du décor (échelles, portes...)
        // dans objetsDecor : on construit une nouvelle liste avec seulement eux
        List<Levier> leviers = new ArrayList<>();
        for (ObjetInteractif objet : objetsDecor) {
            // instanceof teste si l'objet est un Levier
            if (objet instanceof Levier) {
                // (Levier) objet : on indique à Java que cet objet est bien un Levier
                leviers.add((Levier) objet);
            }
        }
        // Liste en lecture seule
        return Collections.unmodifiableList(leviers);
    }

    /* ________________________________________________________________
     * ACTION : Retourne les blocs de porte définis dans le niveau.
     * ________________________________________________________________ */
    public List<BlocPorte> getBlocsPorte() {
        // Même principe que getLeviers : on ne garde que les objets BlocPorte
        List<BlocPorte> blocsPorte = new ArrayList<>();
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof BlocPorte) {
                blocsPorte.add((BlocPorte) objet);
            }
        }
        // Liste en lecture seule
        return Collections.unmodifiableList(blocsPorte);
    }

    /* ________________________________________________________________
     * ACTION : Retourne les blocs mortels définis dans le niveau.
     * ________________________________________________________________ */
    public List<BlocMortel> getBlocsMortels() {
        // Même principe que getLeviers : on ne garde que les objets BlocMortel
        List<BlocMortel> blocsMortels = new ArrayList<>();
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof BlocMortel) {
                blocsMortels.add((BlocMortel) objet);
            }
        }
        // Liste en lecture seule
        return Collections.unmodifiableList(blocsMortels);
    }

    /* ________________________________________________________________
     * ACTION : Retourne les caisses présentes sur la carte.
     * ________________________________________________________________ */
    public List<Caisse> getCaisses() {
        // Les caisses ont leur propre liste (elles bougent) : on la renvoie
        // directement, en lecture seule
        return Collections.unmodifiableList(caisses);
    }

    /* ________________________________________________________________
     * ACTION : Indique si une hitbox peut se poser sur un sol ou une échelle.
     * ________________________________________________________________ */
    public boolean aUnSolSous(int x, int y, int largeurHitbox, int hauteurHitbox) {
        // On teste la hitbox décalée d'1 pixel vers le bas (y - 1) :
        // si elle touche quelque chose de solide (tuile, porte fermée, caisse),
        // c'est qu'il y a un sol juste sous les pieds
        if (collisionHitbox(x, y - 1, largeurHitbox, hauteurHitbox)) {
            return true;
        }
        // Sinon, le haut d'une échelle compte aussi comme un sol
        return estSurSommetEchelle(x, y, largeurHitbox);
    }

    /* ________________________________________________________________
     * ACTION : Détecte un contact exact avec le dessus d'une échelle.
     * ________________________________________________________________ */
    public boolean estSurSommetEchelle(int x, int y, int largeurHitbox) {
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Echelle) {
                // Hauteur du sommet de l'échelle : son bas + sa hauteur (hitbox[1])
                int haut = objet.getPositionY() + objet.getHitbox()[1];
                // Les pieds (y) doivent être EXACTEMENT au niveau du sommet,
                // et la hitbox doit chevaucher l'échelle horizontalement :
                // - x < bord droit de l'échelle
                // - bord droit de la hitbox (x + largeur) > bord gauche de l'échelle
                if (y == haut && x < objet.getPositionX() + objet.getHitbox()[0]
                        && x + largeurHitbox > objet.getPositionX()) {
                    return true;
                }
            }
        }
        // Aucune échelle sous les pieds
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Détecte le franchissement descendant du sommet d'une échelle.
     * [INFO] Retourne la hauteur du sommet franchi, ou Integer.MIN_VALUE
     *        si aucun sommet n'est traversé.
     * ________________________________________________________________ */
    public int getSommetEchelleFranchi(int x, int anciennePositionY, int nouvellePositionY,
            int largeurHitbox) {
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Echelle) {
                // Hauteur du sommet de l'échelle
                int haut = objet.getPositionY() + objet.getHitbox()[1];
                // L'entité était au-dessus (ou pile sur) le sommet et passe en dessous
                // pendant ce déplacement, tout en étant alignée avec l'échelle :
                // elle est en train de traverser le sommet en tombant
                if (anciennePositionY >= haut && nouvellePositionY < haut
                        && x < objet.getPositionX() + objet.getHitbox()[0]
                        && x + largeurHitbox > objet.getPositionX()) {
                    // On renvoie la hauteur du sommet pour qu'Entite
                    // y arrête la chute (sauf si le joueur veut descendre)
                    return haut;
                }
            }
        }
        // Valeur spéciale qui signifie « aucun sommet franchi »
        return Integer.MIN_VALUE;
    }

    /* ________________________________________________________________
     * ACTION : Indique si une hitbox est sur ou dans une échelle.
     * ________________________________________________________________ */
    public boolean estSurOuDansEchelle(int x, int y, int largeurHitbox,
            int hauteurHitbox) {
        // Cas 1 : la hitbox est DANS une échelle (elle la chevauche)
        if (chevaucheEchelle(x, y, largeurHitbox, hauteurHitbox)) {
            return true;
        }
        // Cas 2 : la hitbox est posée SUR le sommet d'une échelle
        // (même test que estSurSommetEchelle)
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof Echelle) {
                int haut = objet.getPositionY() + objet.getHitbox()[1];
                if (y == haut && x < objet.getPositionX() + objet.getHitbox()[0]
                        && x + largeurHitbox > objet.getPositionX()) {
                    return true;
                }
            }
        }
        // Ni dans ni sur une échelle
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Indique si une hitbox chevauche une échelle.
     * ________________________________________________________________ */
    public boolean chevaucheEchelle(int x, int y, int largeurHitbox, int hauteurHitbox) {
        for (ObjetInteractif objet : objetsDecor) {
            // chevaucheRectangle teste si le rectangle (x, y, largeur, hauteur)
            // recouvre au moins un pixel de l'échelle
            if (objet instanceof Echelle
                    && objet.chevaucheRectangle(x, y, largeurHitbox, hauteurHitbox)) {
                return true;
            }
        }
        // Aucune échelle touchée
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Essaie de pousser une caisse et les caisses qui la précèdent.
     * [INFO] direction vaut -1 (gauche) ou 1 (droite) : la caisse avance d'1 pixel.
     * [INFO] Retourne true si la caisse a pu avancer.
     * ________________________________________________________________ */
    public boolean pousserCaisse(Caisse caisse, int direction) {
        // Position x qu'aurait la caisse après avoir avancé d'1 pixel
        int prochainePositionX = caisse.getPositionX() + direction;

        // Si une autre caisse se trouve à cette nouvelle position, il faut
        // d'abord la pousser elle aussi (des caisses collées avancent ensemble)
        for (Caisse autre : caisses) {
            if (autre != caisse && autre.chevaucheRectangle(prochainePositionX,
                    caisse.getPositionY(), caisse.getHitbox()[0], caisse.getHitbox()[1])) {
                // Appel récursif : pousserCaisse s'appelle elle-même sur la caisse suivante.
                // Si celle-ci est bloquée, toute la rangée est bloquée.
                if (!pousserCaisse(autre, direction)) {
                    return false;
                }
            }
        }

        // La caisse ne peut pas entrer dans un mur, une porte fermée
        // ou une caisse qui n'a pas pu bouger
        if (collisionHitboxSansCaisses(prochainePositionX, caisse.getPositionY(),
                caisse.getHitbox()[0], caisse.getHitbox()[1], caisse)) {
            return false;
        }

        // La voie est libre : on déplace la caisse
        caisse.setPositionX(prochainePositionX);
        return true;
    }

    /* ________________________________________________________________
     * ACTION : Teste les collisions d'une hitbox en ignorant une caisse donnée.
     * [INFO] Sert aux caisses : une caisse ne doit pas se bloquer elle-même.
     * ________________________________________________________________ */
    public boolean collisionHitboxSansCaisses(int x, int y, int largeurHitbox,
            int hauteurHitbox, Caisse caisseIgnoree) {
        // D'abord les tuiles solides et les portes fermées
        if (collisionTerrainEtPortes(x, y, largeurHitbox, hauteurHitbox)) {
            return true;
        }
        // Puis toutes les caisses, sauf celle qu'on doit ignorer
        // (sinon la caisse serait toujours « en collision » avec elle-même)
        for (Caisse caisse : caisses) {
            if (caisse != caisseIgnoree
                    && caisse.chevaucheRectangle(x, y, largeurHitbox, hauteurHitbox)) {
                return true;
            }
        }
        // Rien ne bloque
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Retourne les objets du décor interactif hors ingrédients.
     * ________________________________________________________________ */
    public List<ObjetInteractif> getObjetsDecor() {
        // Tous les objets du décor (leviers, portes, échelles, blocs mortels),
        // en lecture seule
        return Collections.unmodifiableList(objetsDecor);
    }

    /* ________________________________________________________________
     * ACTION : Met à jour chaque porte selon le levier et son état initial.
     * [INFO] Seule l’occupation de la même porte retarde sa fermeture.
     * ________________________________________________________________ */
    public void mettreAJourMecanismes(List<Entite> entites) {
        List<BlocPorte> blocsPorte = getBlocsPorte();

        // ===== ÉTAPE 1 : ce que demande chaque levier =====
        for (BlocPorte bloc : blocsPorte) {
            // On cherche le levier qui a le même identifiant que la porte
            boolean actif = false;
            for (ObjetInteractif objet : objetsDecor) {
                // continue : on saute les objets qui ne sont pas des leviers
                if (!(objet instanceof Levier)) {
                    continue;
                }
                Levier levier = (Levier) objet;
                if (levier.getIdMecanisme() == bloc.getIdMecanisme()) {
                    // Levier trouvé : on retient s'il est actionné, puis on arrête de chercher
                    actif = levier.isActive();
                    break;
                }
            }
            // La porte doit être fermée si son état initial et le levier « ne sont pas pareils » :
            // - porte FERMEE au départ + levier inactif -> fermée
            // - porte FERMEE au départ + levier actif   -> ouverte
            // - porte OUVERTE au départ + levier inactif -> ouverte
            // - porte OUVERTE au départ + levier actif   -> fermée
            bloc.definirFermetureDemandee(bloc.isFermeeInitialement() != actif);
        }

        // ===== ÉTAPE 2 : on applique l'état, sans enfermer personne =====
        for (BlocPorte bloc : blocsPorte) {
            // Ouvrir est toujours possible tout de suite
            if (!bloc.estFermetureDemandee()) {
                bloc.definirFerme(false);
                continue;
            }

            // Pour fermer, on vérifie qu'aucune entité (joueur, monstre, caisse)
            // ne se trouve dans la porte. Une même ligne PORTE peut être découpée
            // en plusieurs blocs : on regarde tous les blocs de la même porte (même idPorte)
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

            // Tous les blocs de cette porte se ferment ensemble, sauf si quelqu'un
            // est dedans : dans ce cas ils restent ouverts et on réessaiera au tic suivant
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
        // On ne dessine que si le tileset est chargé et que le numéro existe
        // (sinon tuiles[numero] provoquerait une erreur)
        if (tuiles != null && numero >= 0 && numero < tuiles.length) {
            // drawImage redimensionne la tuile à largeurTuile x hauteurTuile
            contexte.drawImage(tuiles[numero], x, y, largeurTuile, hauteurTuile, null);
        }
    }

    /* ________________________________________________________________
     * ACTION : Retourne les ingrédients déclarés à la suite de la grille.
     * ________________________________________________________________ */
    public List<Ingredient> getIngredients() {
        // Liste en lecture seule des ingrédients lus dans la section INGREDIENTS
        return Collections.unmodifiableList(ingredients);
    }

    /* ________________________________________________________________
     * ACTION : Retourne la largeur en nombre de tuiles.
     * ________________________________________________________________ */
    public int getLargeur() {
        // Nombre de colonnes lu sur la 3e ligne du fichier
        return largeur;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hauteur en nombre de tuiles.
     * ________________________________________________________________ */
    public int getHauteur() {
        // Nombre de lignes lu sur la 3e ligne du fichier
        return hauteur;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la taille d'une tuile en pixels.
     * ________________________________________________________________ */
    public int getTailleTuile() {
        // Côté d'une tuile carrée (32 pixels dans nos niveaux)
        return tailleTuile;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la largeur du décor en pixels.
     * ________________________________________________________________ */
    public int getLargeurPixels() {
        // Nombre de colonnes x taille d'une tuile (ex : 60 x 32 = 1920 px)
        return largeur * tailleTuile;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hauteur du décor en pixels.
     * ________________________________________________________________ */
    public int getHauteurPixels() {
        // Nombre de lignes x taille d'une tuile (ex : 33 x 32 = 1056 px)
        return hauteur * tailleTuile;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le numéro de tuile ligne i, colonne j.
     * ________________________________________________________________ */
    public int getTuile(int i, int j) {
        // i = ligne (depuis le haut), j = colonne (depuis la gauche)
        return decor[i][j];
    }

    /* ________________________________________________________________
     * ACTION : Change le numéro de tuile ligne i, colonne j.
     * [INFO] Une case hors du décor est ignorée.
     * [DEPENDANCE] GestionBDD exporte la map quand elle est modifiée.
     * ________________________________________________________________ */
    public void setTuile(int i, int j, int numero) {
        // On ne fait rien si la case est en dehors de la grille
        // ou si elle contient déjà ce numéro (rien à changer)
        if (i < 0 || i >= hauteur || j < 0 || j >= largeur || decor[i][j] == numero) {
            return;
        }
        // On change la tuile et on note que la map a été modifiée
        // (GestionBDD s'en sert pour savoir qu'il faut la renvoyer)
        decor[i][j] = numero;
        modifie = true;
    }

    /* ________________________________________________________________
     * ACTION : Indique si le décor a changé depuis le dernier export.
     * ________________________________________________________________ */
    public boolean estModifie() {
        // true si setTuile a changé une case depuis le dernier effacerModifie()
        return modifie;
    }

    /* ________________________________________________________________
     * ACTION : Remet à zéro l'indicateur de modification.
     * ________________________________________________________________ */
    public void effacerModifie() {
        // Appelé après l'export : la map est de nouveau « à jour »
        modifie = false;
    }

    /* ________________________________________________________________
     * ACTION : Découpe le tileset en tuiles numérotées ligne par ligne.
     * ________________________________________________________________ */
    private void decouperTileset(BufferedImage tileset) {
        // Nombre de tuiles dans l'image, en largeur et en hauteur
        int nbColonnes = tileset.getWidth() / tailleTuile;
        int nbLignes = tileset.getHeight() / tailleTuile;
        tuiles = new BufferedImage[nbColonnes * nbLignes];

        // Les tuiles sont numérotées de gauche à droite, puis ligne par ligne :
        // 0 1 2 ... (nbColonnes - 1) sur la 1re ligne, puis nbColonnes, ... sur la 2e
        for (int i = 0; i < tuiles.length; i++) {
            // i % nbColonnes = numéro de colonne de la tuile dans l'image
            int x = (i % nbColonnes) * tailleTuile;
            // i / nbColonnes = numéro de ligne de la tuile dans l'image (division entière)
            int y = (i / nbColonnes) * tailleTuile;
            // getSubimage découpe le carré de tailleTuile pixels à partir de (x, y)
            tuiles[i] = tileset.getSubimage(x, y, tailleTuile, tailleTuile);
        }
    }

    /* ________________________________________________________________
     * ACTION : Actualise la map à chaque tour de boucle.
     * [INFO] Le décor reste statique.
     * ________________________________________________________________ */
    public void miseAJour() {
        // Rien à faire pour l'instant : les tuiles ne bougent pas.
        // Les portes sont mises à jour à part, dans mettreAJourMecanismes.
    }

    /* ________________________________________________________________
     * ACTION : Dessine l'image de fond puis le décor tuile par tuile.
     * [INFO] Avec un fond, les tuiles ciel ne sont pas dessinées
     *        pour laisser voir la ville derrière.
     * ________________________________________________________________ */
    public void rendu(Graphics2D contexte) {
        // 1) Le fond (ciel + ville), étiré sur toute la taille de la carte
        if (fond != null) {
            contexte.drawImage(fond, 0, 0, getLargeurPixels(), getHauteurPixels(), null);
        }

        // Pas de tileset chargé : on ne peut pas dessiner les tuiles
        if (tuiles == null) {
            return;
        }

        // 2) Les tuiles : i parcourt les lignes, j parcourt les colonnes
        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {
                // Case de ciel : on la saute pour laisser voir le fond
                if (fond != null && decor[i][j] == TUILE_CIEL) {
                    continue;
                }
                // À l'écran, y part du haut : la ligne i est donc à i * tailleTuile
                contexte.drawImage(tuiles[decor[i][j]], j * tailleTuile, i * tailleTuile, null);
            }
        }
    }

    /* ________________________________________________________________
     * ACTION : Indique si la case ligne i, colonne j bloque les entités.
     * [INFO] Hors du décor, les côtés sont solides, le haut et le bas non.
     * ________________________________________________________________ */
    public boolean estSolide(int i, int j) {
        // À gauche ou à droite de la carte : mur invisible, on ne peut pas sortir
        if (j < 0 || j >= largeur) {
            return true;
        }
        // Au-dessus ou en dessous de la carte : vide, on peut sauter haut ou tomber
        if (i < 0 || i >= hauteur) {
            return false;
        }
        // On vérifie si le numéro de la tuile est parmi ceux traversables (le ciel)
        for (int numero : TUILES_TRAVERSABLES) {
            if (decor[i][j] == numero) {
                return false;
            }
        }
        // Toute autre tuile (trottoir, briques...) est solide
        return true;
    }

    /* ________________________________________________________________
     * ACTION : Indique si une hitbox placée en (x, y) chevauche une tuile solide.
     * [INFO] L'origine de la hitbox est en bas à gauche et y monte.
     * [INFO] On suppose que y = 0 correspond au bas du décor.
     * ________________________________________________________________ */
    public boolean collisionHitbox(int x, int y, int largeurHitbox, int hauteurHitbox) {
        // D'abord les tuiles solides et les portes fermées
        if (collisionTerrainEtPortes(x, y, largeurHitbox, hauteurHitbox)) {
            return true;
        }
        // Puis les caisses, qui bloquent aussi les entités
        for (Caisse caisse : caisses) {
            if (caisse.chevaucheRectangle(x, y, largeurHitbox, hauteurHitbox)) {
                return true;
            }
        }
        // Rien ne bloque
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Teste les tuiles solides et les portes fermées.
     * ________________________________________________________________ */
    private boolean collisionTerrainEtPortes(int x, int y, int largeurHitbox,
            int hauteurHitbox) {
        // ===== 1) Bords de la hitbox en pixels (repère du jeu : y monte) =====
        // "- 1" : une hitbox de 32 pixels qui commence en 0 finit au pixel 31
        int hitboxPixelGauche = x;
        int hitboxPixelDroite = x + largeurHitbox - 1;
        int hitboxPixelBas = y;
        int hitboxPixelHaut = y + hauteurHitbox - 1;

        // ===== 2) Conversion vers le repère du décor (y descend depuis le haut) =====
        // Le haut de la hitbox dans le jeu correspond à la plus PETITE valeur dans le décor
        int hitboxPixelHautDecor = getHauteurPixels() - 1 - hitboxPixelHaut;
        int hitboxPixelBasDecor = getHauteurPixels() - 1 - hitboxPixelBas;

        // ===== 3) Tuiles couvertes par la hitbox =====
        // floorDiv divise en arrondissant vers le bas, même pour les nombres négatifs
        // (ex : -1 / 32 donne -1 et non 0), utile quand l'entité sort de la carte
        int hitboxTuileGauche = Math.floorDiv(hitboxPixelGauche, tailleTuile);
        int hitboxTuileDroite = Math.floorDiv(hitboxPixelDroite, tailleTuile);
        int hitboxTuileHaut = Math.floorDiv(hitboxPixelHautDecor, tailleTuile);
        int hitboxTuileBas = Math.floorDiv(hitboxPixelBasDecor, tailleTuile);

        // ===== 4) Une seule tuile solide sous la hitbox suffit à bloquer =====
        for (int i = hitboxTuileHaut; i <= hitboxTuileBas; i++) {
            for (int j = hitboxTuileGauche; j <= hitboxTuileDroite; j++) {
                if (estSolide(i, j)) {
                    return true;
                }
            }
        }

        // ===== 5) Les portes fermées bloquent aussi =====
        // isCollisionJoueur() vaut true seulement quand la porte est fermée
        for (ObjetInteractif objet : objetsDecor) {
            if (objet instanceof BlocPorte && objet.isCollisionJoueur()
                    && objet.chevaucheRectangle(x, y, largeurHitbox, hauteurHitbox)) {
                return true;
            }
        }

        // Ni tuile solide ni porte fermée
        return false;
    }
}
