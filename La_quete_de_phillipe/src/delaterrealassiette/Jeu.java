package delaterrealassiette;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Logique d'une partie : niveau courant, joueur, monstres, caisses,
 * ingrédients et progression d'un niveau à l'autre.
 *
 * Fonctionnement : la fenêtre appelle miseAJour() puis rendu() à chaque tick.
 * Un tick enchaîne, dans cet ordre : intentions du joueur, compétences (soin,
 * levier), portes, physique des entités, dégâts des monstres, mort
 * éventuelle, ramassage des ingrédients, puis passage au niveau suivant
 * quand la recette est complète. Après la mort, le message reste affiché
 * quelques ticks puis le niveau est rechargé à neuf ; après le dernier
 * niveau, la partie est figée en victoire.
 *
 * Multijoueur : Jeu ne fait aucun accès à la base. Il tient deux « boîtes » :
 * - sortante : leviers basculés et ingrédients ramassés localement, rangés
 *   par niveau, que le thread de SynchronisationBDD vient lire, publie, puis
 *   confirme pour les retirer ;
 * - entrante : dernier état partagé reçu de la base, simplement mémorisé par
 *   le thread réseau et appliqué au début du tick suivant, sur le thread du
 *   jeu. Les méthodes partagées entre les deux threads sont synchronized.
 */
public class Jeu {
    /** Position de départ si le niveau ne déclare pas de SPAWN. */
    private static final int departX = 32;
    private static final int departY = 64;

    private String fichierNiveau;
    private int numeroNiveau;
    private Map carte;
    private Joueur joueur;
    /** Joueur, monstres et caisses : tout ce qui est mis à jour et dessiné. */
    private List<Entite> entites;
    private List<Ingredient> ingredients;
    private InventaireRecette inventaireRecette;
    private boolean victoire;
    private static final int nombreTicsAvantReapparition = 30;
    private int ticsAvantReapparition;

    // Multijoueur, boîte sortante : niveau -> changements locaux pas encore confirmés
    private final java.util.Map<Integer, java.util.Map<Integer, Boolean>> leviersModifies = new HashMap<>();
    private final java.util.Map<Integer, Set<String>> ingredientsModifies = new HashMap<>();
    // Multijoueur, boîte entrante : dernier état reçu, appliqué au prochain tick
    private java.util.Map<Integer, Boolean> leviersRecus = new HashMap<>();
    private Set<String> ingredientsRecus = new HashSet<>();
    private int niveauEtatsRecus;
    private int niveauPartageDemande;
    private boolean etatMultijoueurEnAttente;
    /** Traces de synchronisation, activées avec -Dlaquete.sync.debug=true. */
    private static final boolean LOG_SYNC = Boolean.getBoolean("laquete.sync.debug");

    /**
     * @param fichierNiveau niveau de départ, au format "niveauN.txt"
     */
    public Jeu(String fichierNiveau) {
        this.fichierNiveau = fichierNiveau;
        numeroNiveau = extraireNumeroNiveau(fichierNiveau);
        chargerNiveau(null);
    }

    /**
     * (Re)construit tout le niveau courant : carte, joueur, entités,
     * ingrédients et recette.
     *
     * @param joueurPrecedent joueur à transférer (PV, score, chef), ou null
     *                        pour un joueur neuf
     */
    private void chargerNiveau(Joueur joueurPrecedent) {
        carte = new Map(fichierNiveau);
        joueur = joueurPrecedent == null
                ? new Joueur(carte, "joueurtesthitbox")
                : new Joueur(carte, joueurPrecedent);
        int[] positionDepart = carte.getPositionDepart();
        if (positionDepart == null) {
            positionDepart = new int[]{departX, departY};
        }
        joueur.definirPosition(positionDepart[0], positionDepart[1]);
        entites = new ArrayList<>();
        entites.add(joueur);
        entites.addAll(carte.getMonstres());
        entites.addAll(carte.getCaisses());
        ingredients = new ArrayList<>(carte.getIngredients());
        inventaireRecette = new InventaireRecette(ingredients);
    }

    /** "niveau2.txt" -> 2 */
    private int extraireNumeroNiveau(String nomFichier) {
        String nom = new java.io.File(nomFichier).getName();
        return Integer.parseInt(nom.replace("niveau", "").replace(".txt", ""));
    }

    /** Tick sans aucune touche appuyée. */
    public void miseAJour() {
        miseAJour(new boolean[10]);
    }

    /**
     * Un tick de jeu (ordre décrit en tête de classe).
     *
     * @param etatBoutons état des touches, indexé par les constantes Joueur.touche*
     */
    public synchronized void miseAJour(boolean[] etatBoutons) {
        // D'abord ce qui vient des autres joueurs
        appliquerNiveauPartageDemande();
        appliquerEtatsRecus();
        if (victoire) {
            return;
        }
        // Écran de mort : on attend puis on recharge le niveau
        if (ticsAvantReapparition > 0) {
            ticsAvantReapparition--;
            if (ticsAvantReapparition == 0) {
                reset();
            }
            return;
        }

        // Compétences
        joueur.miseAJourIntentions(etatBoutons);
        if (joueur.consommerSoinDemande()) {
            appliquerSoinMaite();
        }
        if (joueur.consommerActivationLevier()) {
            for (Levier levier : carte.getLeviers()) {
                if (levier.estAProximite(joueur)) {
                    levier.basculer();
                    leviersModifies.computeIfAbsent(numeroNiveau, k -> new HashMap<>())
                            .put(levier.getIdMecanisme(), levier.isActive());
                    break;
                }
            }
        }

        // Monde : portes, puis caisses avant les autres entités qui s'y appuient
        carte.mettreAJourMecanismes(entites);
        carte.miseAJour();
        for (Caisse caisse : carte.getCaisses()) {
            caisse.mettreAJour();
        }
        for (Entite entite : entites) {
            if (entite instanceof Caisse) {
                continue;
            }
            entite.mettreAJour();
        }

        // Dégâts et mort (vide sous la carte ou bloc mortel = mort immédiate)
        for (Entite entite : entites) {
            if (entite instanceof Monstre) {
                ((Monstre) entite).infligerDegatsAuContact(joueur);
            }
        }
        if (estTombeDansLeVide() || estEnContactAvecBlocMortel()) {
            joueur.modifierPointsDeVie(-joueur.getPointsDeVie());
        }
        if (joueur.estMort()) {
            ticsAvantReapparition = nombreTicsAvantReapparition;
            return;
        }

        // Ramassage des ingrédients (noté aussi pour la synchronisation)
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isActif()) {
                continue;
            }
            for (Entite entite : entites) {
                if (entite instanceof Joueur
                        && ingredient.enCollisionAvec(entite)) {
                    ingredient.interagir((Joueur) entite);
                    ingredientsModifies.computeIfAbsent(numeroNiveau, k -> new HashSet<>())
                            .add(ingredient.getIdentifiantMultijoueur());
                    inventaireRecette.ajouter(ingredient.getTypeIngredient());
                    break;
                }
            }
        }
        if (inventaireRecette.estComplete()) {
            passerAuNiveauSuivant();
        }
    }

    /**
     * Soin de Maite : +20 % des PV max pour chaque joueur vivant qui la
     * touche (elle comprise). La recharge est consommée même si personne
     * n'est soigné.
     */
    private void appliquerSoinMaite() {
        int soin = joueur.getPointsDeVieMax() / 5;
        for (Entite entite : entites) {
            if (!(entite instanceof Joueur)) {
                continue;
            }
            Joueur joueurCible = (Joueur) entite;
            if (!joueurCible.estMort() && chevauchent(joueur, joueurCible)) {
                joueurCible.modifierPointsDeVie(soin);
            }
        }
    }

    private boolean chevauchent(Entite premiereEntite, Entite secondeEntite) {
        int[] premiereHitbox = premiereEntite.getHitbox();
        int[] secondeHitbox = secondeEntite.getHitbox();
        return premiereEntite.getPositionX() < secondeEntite.getPositionX() + secondeHitbox[0]
                && secondeEntite.getPositionX() < premiereEntite.getPositionX() + premiereHitbox[0]
                && premiereEntite.getPositionY() < secondeEntite.getPositionY() + secondeHitbox[1]
                && secondeEntite.getPositionY() < premiereEntite.getPositionY() + premiereHitbox[1];
    }

    /** @return true si le joueur est entièrement sous le bas de la carte (y &lt; 0) */
    private boolean estTombeDansLeVide() {
        int hauteurHitbox = joueur.getHitbox()[1];
        return joueur.getPositionY() + hauteurHitbox < 0;
    }

    private boolean estEnContactAvecBlocMortel() {
        for (BlocMortel blocMortel : carte.getBlocsMortels()) {
            if (blocMortel.enCollisionAvec(joueur)) {
                return true;
            }
        }
        return false;
    }

    /** Recommence le niveau courant de zéro (joueur neuf, objets replacés). */
    private void reset() {
        chargerNiveau(null);
    }

    /** Charge le niveau suivant en gardant le joueur, ou déclare la victoire après le niveau 3. */
    private void passerAuNiveauSuivant() {
        if (numeroNiveau >= 3) {
            victoire = true;
            return;
        }
        numeroNiveau++;
        fichierNiveau = "niveau" + numeroNiveau + ".txt";
        Joueur joueurPrecedent = joueur;
        chargerNiveau(joueurPrecedent);
    }

    /**
     * Dessine le niveau : décor, entités, objets, caisses puis interface.
     * Chaque position du jeu (y vers le haut) est convertie en position
     * écran (y vers le bas) : yEcran = hauteurCarte - y - hauteurHitbox.
     * Les ingrédients sont dessinés à part (dessinerIngredients).
     */
    public void rendu(Graphics2D contexte) {
        carte.rendu(contexte);
        for (Entite entite : entites) {
            if (!(entite instanceof Caisse)) {
                int[] hitbox = entite.getHitbox();
                int xEntite = entite.getPositionX();
                int yEntite = carte.getHauteurPixels() - entite.getPositionY() - hitbox[1];
                entite.dessiner(contexte, xEntite, yEntite);
            }
        }
        for (ObjetInteractif objet : carte.getObjetsDecor()) {
            int[] hitbox = objet.getHitbox();
            int yEcran = carte.getHauteurPixels() - objet.getPositionY() - hitbox[1];
            objet.dessiner(contexte, objet.getPositionX(), yEcran);
        }
        for (Caisse caisse : carte.getCaisses()) {
            int[] hitbox = caisse.getHitbox();
            int yEcran = carte.getHauteurPixels() - caisse.getPositionY() - hitbox[1];
            caisse.dessiner(contexte, caisse.getPositionX(), yEcran);
        }
        dessinerPointsDeVie(contexte);
        // Voile noir et message pendant l'attente avant réapparition
        if (ticsAvantReapparition > 0) {
            Composite compositePrecedent = contexte.getComposite();
            contexte.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));
            contexte.setColor(Color.BLACK);
            contexte.fillRect(0, 0, carte.getLargeurPixels(), carte.getHauteurPixels());
            contexte.setComposite(compositePrecedent);
            contexte.setColor(Color.WHITE);
            contexte.setFont(new Font("SansSerif", Font.BOLD, 54));
            String message = "VOUS ÊTES MORT";
            int largeurMessage = contexte.getFontMetrics().stringWidth(message);
            contexte.drawString(message,
                    (carte.getLargeurPixels() - largeurMessage) / 2,
                    carte.getHauteurPixels() / 2);
        }
    }

    /** Barre de vie (verte, rouge sous un tiers) et jauge de recharge du soin. */
    private void dessinerPointsDeVie(Graphics2D contexte) {
        int largeurBarre = 240;
        int hauteurBarre = 20;
        int largeurRemplie = largeurBarre * joueur.getPointsDeVie()
                / joueur.getPointsDeVieMax();
        contexte.setColor(Color.BLACK);
        contexte.fillRect(18, 18, largeurBarre + 4, hauteurBarre + 38);
        contexte.setColor(Color.DARK_GRAY);
        contexte.fillRect(20, 40, largeurBarre, hauteurBarre);
        contexte.setColor(joueur.getPointsDeVie() > joueur.getPointsDeVieMax() / 3
                ? new Color(70, 205, 90) : new Color(230, 65, 55));
        contexte.fillRect(20, 40, largeurRemplie, hauteurBarre);
        contexte.setColor(Color.WHITE);
        contexte.setFont(new Font("SansSerif", Font.BOLD, 18));
        contexte.drawString("PV " + joueur.getPointsDeVie() + " / "
                + joueur.getPointsDeVieMax(), 20, 34);
        int largeurRecharge = (int) (largeurBarre * joueur.getProgressionRechargeSoin());
        contexte.setColor(Color.DARK_GRAY);
        contexte.fillRect(20, 68, largeurBarre, 8);
        contexte.setColor(new Color(105, 190, 235));
        contexte.fillRect(20, 68, largeurRecharge, 8);
    }

    // ----- Multijoueur : boîte sortante (lue par le thread de synchronisation) -----

    /** @return niveaux ayant des changements locaux non encore publiés */
    public synchronized Set<Integer> getNiveauxModifies() {
        Set<Integer> niveaux = new HashSet<>(leviersModifies.keySet());
        niveaux.addAll(ingredientsModifies.keySet());
        return niveaux;
    }

    /** @return copie des leviers modifiés localement (id mécanisme -&gt; actif) pour ce niveau */
    public synchronized java.util.Map<Integer, Boolean> getLeviersModifies(int niveau) {
        java.util.Map<Integer, Boolean> copie = leviersModifies.get(niveau);
        return copie == null ? new HashMap<>() : new HashMap<>(copie);
    }

    /** @return copie des identifiants d'ingrédients ramassés localement pour ce niveau */
    public synchronized Set<String> getIngredientsModifies(int niveau) {
        Set<String> copie = ingredientsModifies.get(niveau);
        return copie == null ? new HashSet<>() : new HashSet<>(copie);
    }

    /**
     * Retire de la boîte sortante ce que la base a enregistré. Un levier
     * rebasculé entre-temps (valeur différente) reste en attente.
     */
    public synchronized void confirmerChangementsMultijoueur(int niveau,
            java.util.Map<Integer, Boolean> leviers, Set<String> ingredients) {
        java.util.Map<Integer, Boolean> leviersEnAttente = leviersModifies.get(niveau);
        if (leviersEnAttente != null) {
            for (java.util.Map.Entry<Integer, Boolean> entree : leviers.entrySet()) {
                if (entree.getValue().equals(leviersEnAttente.get(entree.getKey()))) {
                    leviersEnAttente.remove(entree.getKey());
                }
            }
            if (leviersEnAttente.isEmpty()) leviersModifies.remove(niveau);
        }
        Set<String> ingredientsEnAttente = ingredientsModifies.get(niveau);
        if (ingredientsEnAttente != null) {
            ingredientsEnAttente.removeAll(ingredients);
            if (ingredientsEnAttente.isEmpty()) ingredientsModifies.remove(niveau);
        }
    }

    // ----- Multijoueur : boîte entrante (remplie par le thread, appliquée par le jeu) -----

    /**
     * Mémorise l'état partagé lu dans la base (niveau de la partie, leviers,
     * ingrédients ramassés). Appelée depuis le thread réseau : rien n'est
     * modifié dans le niveau ici, tout est appliqué au tick suivant.
     */
    public synchronized void appliquerEtatMultijoueur(int niveau,
            java.util.Map<Integer, Boolean> leviers, Set<String> ingredientsRamasses) {
        if (niveau < 1 || niveau > 3) {
            if (LOG_SYNC) System.out.println("[SYNC] État ignoré: niveau_id invalide=" + niveau);
            return;
        }
        niveauPartageDemande = niveau;
        niveauEtatsRecus = niveau;
        etatMultijoueurEnAttente = true;
        if (LOG_SYNC) System.out.println("[SYNC] Instantané reçu niveau_id=" + niveau
                + (niveau == numeroNiveau ? " prêt à appliquer" : "; changement de niveau en attente"));
        leviersRecus = new HashMap<>(leviers);
        ingredientsRecus = new HashSet<>(ingredientsRamasses);
    }

    /**
     * Si un autre joueur est passé à un niveau supérieur, on le rejoint.
     * Un niveau inférieur (information périmée) ou invalide est ignoré.
     */
    private void appliquerNiveauPartageDemande() {
        if (niveauPartageDemande <= 0 || niveauPartageDemande == numeroNiveau) return;
        if (niveauPartageDemande < numeroNiveau) {
            if (LOG_SYNC) System.out.println("[SYNC] Niveau partagé périmé ignoré: local="
                    + numeroNiveau + ", reçu=" + niveauPartageDemande);
            niveauPartageDemande = 0;
            return;
        }
        if (niveauPartageDemande < 1 || niveauPartageDemande > 3) {
            if (LOG_SYNC) System.out.println("[SYNC] Niveau partagé ignoré, valeur invalide: " + niveauPartageDemande);
            niveauPartageDemande = 0;
            return;
        }
        int ancien = numeroNiveau;
        numeroNiveau = niveauPartageDemande;
        fichierNiveau = "niveau" + numeroNiveau + ".txt";
        chargerNiveau(joueur);
        if (LOG_SYNC) System.out.println("[SYNC] Niveau appliqué sur le fil du jeu: " + ancien + " -> " + numeroNiveau);
    }

    /**
     * Applique les leviers et ingrédients reçus s'ils concernent le niveau
     * affiché. Ils ne sont pas ajoutés à la boîte sortante, sinon ils
     * seraient republiés indéfiniment.
     */
    private void appliquerEtatsRecus() {
        if (!etatMultijoueurEnAttente || niveauEtatsRecus != numeroNiveau) return;
        etatMultijoueurEnAttente = false;
        int nbLeviers = 0;
        for (Levier levier : carte.getLeviers()) {
            Boolean actif = leviersRecus.get(levier.getIdMecanisme());
            if (actif != null) {
                levier.definirActif(actif);
                nbLeviers++;
            }
        }
        int nbIngredients = 0;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isActif()
                    && ingredientsRecus.contains(ingredient.getIdentifiantMultijoueur())) {
                ingredient.interagir(joueur);
                inventaireRecette.ajouter(ingredient.getTypeIngredient());
                nbIngredients++;
            }
        }
        if (LOG_SYNC) System.out.println("[SYNC] État appliqué niveau_id=" + numeroNiveau
                + ": leviers=" + nbLeviers + ", ingrédients=" + nbIngredients);
    }

    // ----- Accesseurs -----

    public Map getCarte() {
        return carte;
    }

    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public InventaireRecette getInventaireRecette() {
        return inventaireRecette;
    }

    public int getHauteurCartePixels() {
        return carte.getHauteurPixels();
    }

    public synchronized Joueur getJoueur() {
        return joueur;
    }

    /** Dessine les ingrédients pas encore ramassés (même conversion de repère que rendu()). */
    public void dessinerIngredients(Graphics2D contexte) {
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isActif()) {
                continue;
            }
            int yEcran = carte.getHauteurPixels() - ingredient.getPositionY()
                    - ingredient.getHitbox()[1];
            ingredient.dessiner(contexte, ingredient.getPositionX(), yEcran);
        }
    }

    /** @return true une fois le dernier niveau terminé */
    public boolean isVictoire() {
        return victoire;
    }

    public synchronized int getNumeroNiveau() {
        return numeroNiveau;
    }
}
