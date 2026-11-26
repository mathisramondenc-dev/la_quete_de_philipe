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

/* ________________________________________________________________
 * ACTION : Gère la carte, le joueur, les ingrédients et la progression.
 * [DEPENDANCE] FenetreDeJeu appelle miseAJour et rendu à chaque tic.
 * ________________________________________________________________ */
public class Jeu {
    private static final int departX = 32;
    private static final int departY = 64;

    private String fichierNiveau;
    private int numeroNiveau;
    private Map carte;
    private Joueur joueur;
    private List<Entite> entites;
    private List<Ingredient> ingredients;
    private InventaireRecette inventaireRecette;
    private boolean victoire;
    private static final int nombreTicsAvantReapparition = 30;
    private int ticsAvantReapparition;
    // Changements locaux en attente d'envoi par le thread JDBC.
    private final java.util.Map<Integer, java.util.Map<Integer, Boolean>> leviersModifies = new HashMap<>();
    private final java.util.Map<Integer, Set<String>> ingredientsModifies = new HashMap<>();
    private java.util.Map<Integer, Boolean> leviersRecus = new HashMap<>();
    private Set<String> ingredientsRecus = new HashSet<>();
    private int niveauEtatsRecus;
    private int niveauPartageDemande;
    private boolean etatMultijoueurEnAttente;
    private static final boolean LOG_SYNC = Boolean.getBoolean("laquete.sync.debug");

    /* ________________________________________________________________
     * ACTION : Charge un niveau choisi et initialise sa recette.
     * [INFO] Le niveau choisi devient le point de départ de la progression.
     * ________________________________________________________________ */
    public Jeu(String fichierNiveau) {
        this.fichierNiveau = fichierNiveau;
        numeroNiveau = extraireNumeroNiveau(fichierNiveau);
        chargerNiveau(null);
    }

    /* ________________________________________________________________
     * ACTION : Charge la carte, la recette et le joueur de départ.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Extrait le numéro du niveau depuis son nom de fichier.
     * ________________________________________________________________ */
    private int extraireNumeroNiveau(String nomFichier) {
        String nom = new java.io.File(nomFichier).getName();
        return Integer.parseInt(nom.replace("niveau", "").replace(".txt", ""));
    }

    /* ________________________________________________________________
     * ACTION : Met à jour le joueur, ramasse les ingrédients et progresse.
     * [INFO] Une seule transition est effectuée après la collecte complète.
     * ________________________________________________________________ */
    public void miseAJour() {
        miseAJour(new boolean[10]);
    }

    /* ________________________________________________________________
     * ACTION : Met à jour le joueur, les mécanismes, les entités et la recette.
     * [INFO] La mort est affichée avant le respawn, après traitement des dégâts.
     * [DEPENDANCE] Monstre utilise la collision commune d'ElementDeJeu.
     * ________________________________________________________________ */
    public synchronized void miseAJour(boolean[] etatBoutons) {
        appliquerNiveauPartageDemande();
        appliquerEtatsRecus();
        if (victoire) {
            return;
        }
        if (ticsAvantReapparition > 0) {
            ticsAvantReapparition--;
            if (ticsAvantReapparition == 0) {
                reset();
            }
            return;
        }
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

    /* ________________________________________________________________
     * ACTION : Soigne Maite et les joueurs vivants dans sa hitbox.
     * [INFO] Le soin vaut 20 pour cent des PV max, sans dépasser ce maximum.
     *        La demande a déjà consommé la recharge, même si personne n'est soigné.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Vérifie le chevauchement de deux hitbox.
     * ________________________________________________________________ */
    private boolean chevauchent(Entite premiereEntite, Entite secondeEntite) {
        int[] premiereHitbox = premiereEntite.getHitbox();
        int[] secondeHitbox = secondeEntite.getHitbox();
        return premiereEntite.getPositionX() < secondeEntite.getPositionX() + secondeHitbox[0]
                && secondeEntite.getPositionX() < premiereEntite.getPositionX() + premiereHitbox[0]
                && premiereEntite.getPositionY() < secondeEntite.getPositionY() + secondeHitbox[1]
                && secondeEntite.getPositionY() < premiereEntite.getPositionY() + premiereHitbox[1];
    }

    /* ________________________________________________________________
     * ACTION : Indique si le joueur est entièrement passé sous la carte.
     * [INFO] y = 0 correspond au bas du décor, le vide est donc en y négatif.
     * ________________________________________________________________ */
    private boolean estTombeDansLeVide() {
        int hauteurHitbox = joueur.getHitbox()[1];
        return joueur.getPositionY() + hauteurHitbox < 0;
    }

    /* ________________________________________________________________
     * ACTION : Indique si le joueur touche un bloc mortel.
     * ________________________________________________________________ */
    private boolean estEnContactAvecBlocMortel() {
        for (BlocMortel blocMortel : carte.getBlocsMortels()) {
            if (blocMortel.enCollisionAvec(joueur)) {
                return true;
            }
        }
        return false;
    }

    /* ________________________________________________________________
     * ACTION : Relance le niveau actuel comme au premier lancement.
     * [INFO] Carte, ingrédients, inventaire et joueur sont recréés à neuf.
     * ________________________________________________________________ */
    private void reset() {
        chargerNiveau(null);
    }

    /* ________________________________________________________________
     * ACTION : Charge le niveau suivant ou verrouille l’état de victoire.
     * [INFO] Le nouvel inventaire est initialisé une seule fois avec la carte.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Dessine le décor puis les entités du niveau actif.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Affiche les points de vie du joueur.
     * ________________________________________________________________ */
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

    /** Retourne les niveaux pour lesquels des actions attendent leur envoi. */
    public synchronized Set<Integer> getNiveauxModifies() {
        Set<Integer> niveaux = new HashSet<>(leviersModifies.keySet());
        niveaux.addAll(ingredientsModifies.keySet());
        return niveaux;
    }

    /** Copie les états des leviers locaux d'un niveau à publier dans la base. */
    public synchronized java.util.Map<Integer, Boolean> getLeviersModifies(int niveau) {
        java.util.Map<Integer, Boolean> copie = leviersModifies.get(niveau);
        return copie == null ? new HashMap<>() : new HashMap<>(copie);
    }

    /** Copie les identifiants d'ingrédients d'un niveau à publier dans la base. */
    public synchronized Set<String> getIngredientsModifies(int niveau) {
        Set<String> copie = ingredientsModifies.get(niveau);
        return copie == null ? new HashSet<>() : new HashSet<>(copie);
    }

    /** Confirme les changements de ce niveau que la base vient d'accepter. */
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

    /** Met en attente l'état partagé; son application sera faite sur le thread du jeu. */
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

    /** Applique le niveau partagé sur le fil du jeu, jamais depuis le thread JDBC. */
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

    /** Applique les états reçus sans les republier comme des changements locaux. */
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

    /* ________________________________________________________________
     * ACTION : Retourne la carte du niveau actif.
     * ________________________________________________________________ */
    public Map getCarte() {
        return carte;
    }

    /* ________________________________________________________________
     * ACTION : Retourne les ingrédients du niveau actif.
     * ________________________________________________________________ */
    public List<Ingredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    /* ________________________________________________________________
     * ACTION : Retourne l’inventaire actif.
     * ________________________________________________________________ */
    public InventaireRecette getInventaireRecette() {
        return inventaireRecette;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hauteur de la carte en pixels.
     * ________________________________________________________________ */
    public int getHauteurCartePixels() {
        return carte.getHauteurPixels();
    }

    /* ________________________________________________________________
     * ACTION : Retourne le joueur contrôlé.
     * ________________________________________________________________ */
    public synchronized Joueur getJoueur() {
        return joueur;
    }

    /* ________________________________________________________________
     * ACTION : Dessine les ingrédients encore actifs.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Indique si le dernier niveau est terminé.
     * ________________________________________________________________ */
    public boolean isVictoire() {
        return victoire;
    }

    /* ________________________________________________________________
     * ACTION : Retourne l’index du niveau actuellement actif.
     * ________________________________________________________________ */
    public synchronized int getNumeroNiveau() {
        return numeroNiveau;
    }
}
