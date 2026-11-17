package delaterrealassiette;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    // === MULTIJOUEUR === Configuration de session et file de collectes à publier.
    private InfosPartie infosPartie;
    private final java.util.concurrent.ConcurrentLinkedQueue<DemandeCollecte> collectesEnAttente = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final java.util.Set<String> collectesOptimistes = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> collectesLocalesConfirmees = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> collectesPartageesConfirmees = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile boolean recettePartageeComplete;
    private volatile boolean mondeIngredientsPret;

    static final class DemandeCollecte {
        final int niveau, id;
        final TypeIngredient type;
        DemandeCollecte(int niveau, int id, TypeIngredient type) { this.niveau=niveau; this.id=id; this.type=type; }
    }

    private final java.util.concurrent.ConcurrentLinkedQueue<String> leviersEnAttente = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final java.util.concurrent.ConcurrentLinkedQueue<String> degatsEnAttente = new java.util.concurrent.ConcurrentLinkedQueue<>();


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
        int indexMonstre = 0;
        for (Map.PositionMonstre positionMonstre : carte.getPositionsMonstres()) {
            if ("RAT".equals(positionMonstre.getType())) {
                Monstre monstre = Monstre.creerRat(carte, positionMonstre.getPositionX(), positionMonstre.getPositionY());
                monstre.definirIndexMulti(indexMonstre++);
                monstre.definirAutoriteIA(infosPartie == null || !infosPartie.estMultijoueur() || infosPartie.estHebergeur());
                entites.add(monstre);
            }
        }
        ingredients = new ArrayList<>(carte.getIngredients());
        inventaireRecette = new InventaireRecette(ingredients);
        recettePartageeComplete = false;
        mondeIngredientsPret = infosPartie == null || !infosPartie.estMultijoueur();
        int indexLevier=0;
        for (Levier levier:carte.getLeviers()) levier.definirIndexMulti(indexLevier++);
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
        miseAJour(new boolean[9]);
    }

    /* ________________________________________________________________
     * ACTION : Met à jour le joueur, les mécanismes, les entités et la recette.
     * [INFO] La mort est affichée avant le respawn, après traitement des dégâts.
     * [DEPENDANCE] Monstre utilise la collision commune d'ElementDeJeu.
     * ________________________________________________________________ */
    public void miseAJour(boolean[] etatBoutons) {
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
        if (joueur.consommerActivationLevier()) {
            for (Levier levier : carte.getLeviers()) {
                if (levier.estAProximite(joueur)) {
                    levier.basculer();
                    // === MULTIJOUEUR === file l’état demandé, publié par le thread JDBC.
                    if (infosPartie != null && infosPartie.estMultijoueur()) leviersEnAttente.add(levier.getIndexMulti()+":"+levier.isActive());
                    break;
                }
            }
        }
        carte.mettreAJourMecanismes(entites);
        carte.miseAJour();
        for (Entite entite : entites) {
            if (entite instanceof Monstre && infosPartie != null && infosPartie.estMultijoueur() && !infosPartie.estHebergeur()) continue;
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
        if (!mondeIngredientsPret) return;
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isActif()) {
                continue;
            }
            for (Entite entite : entites) {
                if (entite instanceof Joueur
                        && ingredient.enCollisionAvec(entite)) {
                    ingredient.interagir((Joueur) entite);
                    inventaireRecette.ajouter(ingredient.getTypeIngredient());
                    if (infosPartie != null && infosPartie.estMultijoueur()) {
                        int id = ingredients.indexOf(ingredient);
                        collectesOptimistes.add(cleCollecte(numeroNiveau, id));
                        collectesEnAttente.add(new DemandeCollecte(numeroNiveau, id, ingredient.getTypeIngredient()));
                    }
                    break;
                }
            }
        }
        if (inventaireRecette.estComplete()
                && (infosPartie == null || !infosPartie.estMultijoueur() || recettePartageeComplete)) {
            passerAuNiveauSuivant();
        }
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
            int[] hitbox = entite.getHitbox();
            int xEntite = entite.getPositionX();
            int yEntite = carte.getHauteurPixels() - entite.getPositionY() - hitbox[1];
            entite.dessiner(contexte, xEntite, yEntite);
        }
        for (ObjetInteractif objet : carte.getObjetsDecor()) {
            int[] hitbox = objet.getHitbox();
            int yEcran = carte.getHauteurPixels() - objet.getPositionY() - hitbox[1];
            objet.dessiner(contexte, objet.getPositionX(), yEcran);
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
        contexte.fillRect(18, 18, largeurBarre + 4, hauteurBarre + 28);
        contexte.setColor(Color.DARK_GRAY);
        contexte.fillRect(20, 40, largeurBarre, hauteurBarre);
        contexte.setColor(joueur.getPointsDeVie() > joueur.getPointsDeVieMax() / 3
                ? new Color(70, 205, 90) : new Color(230, 65, 55));
        contexte.fillRect(20, 40, largeurRemplie, hauteurBarre);
        contexte.setColor(Color.WHITE);
        contexte.setFont(new Font("SansSerif", Font.BOLD, 18));
        contexte.drawString("PV " + joueur.getPointsDeVie() + " / "
                + joueur.getPointsDeVieMax(), 20, 34);
    }

    /* ________________________________________________________________
     * ACTION : Retourne la carte du niveau actif.
     * ________________________________________________________________ */
    // === MULTIJOUEUR === Appelé par FenetreDeJeu avant de démarrer la synchronisation.
    public void configurerMultijoueur(InfosPartie infos) {
        infosPartie = infos;
        for (Entite entite : entites) if (entite instanceof Monstre)
            ((Monstre) entite).definirAutoriteIA(!infos.estMultijoueur() || infos.estHebergeur());
    }
    public InfosPartie getInfosPartie() { return infosPartie; }
    public java.util.Queue<DemandeCollecte> getCollectesEnAttente() { return collectesEnAttente; }
    private static String cleCollecte(int niveau, int id) { return niveau + ":" + id; }
    public void resultatCollecte(int niveau, int id, boolean acceptee, boolean refuseeParAutre) {
        String cle = cleCollecte(niveau, id);
        if (acceptee) {
            collectesOptimistes.remove(cle);
            collectesLocalesConfirmees.add(cle);
        } else if (refuseeParAutre) {
            // La BDD a explicitement confirmé que l'ingrédient appartient à un autre joueur.
            collectesOptimistes.remove(cle);
            collectesLocalesConfirmees.remove(cle);
            collectesPartageesConfirmees.add(cle);
        } else {
            return; // Aucun refus définitif : conserver l'état optimiste.
        }
        if (niveau == numeroNiveau && id >= 0 && id < ingredients.size()) {
            recalculerInventairePartage();
            Ingredient g = ingredients.get(id);
            g.definirActif(!(collectesOptimistes.contains(cle) || collectesLocalesConfirmees.contains(cle)
                    || collectesPartageesConfirmees.contains(cle)));
        }
    }
    public void appliquerIngredientsPartages(List<GestionBDD.EtatIngredient> etats,
            java.util.Map<TypeIngredient,Integer> quantites) {
        if (infosPartie == null || !infosPartie.estMultijoueur() || etats.isEmpty()) return;
        java.util.Map<TypeIngredient,Integer> requises = new java.util.EnumMap<>(TypeIngredient.class);
        for (GestionBDD.EtatIngredient e : etats) {
            requises.put(e.type, requises.getOrDefault(e.type, 0) + 1);
            if (e.ramasse) collectesPartageesConfirmees.add(cleCollecte(numeroNiveau, e.id));
            if (e.id < 0 || e.id >= ingredients.size()) continue;
            Ingredient ancien = ingredients.get(e.id);
            TypeIngredient type = e.type;
            if (ancien.getTypeIngredient() != type) {
                ancien = new Ingredient(type, e.x, e.y);
                ingredients.set(e.id, ancien);
            } else {
                ancien.definirPosition(e.x, e.y);
            }
            String cle = cleCollecte(numeroNiveau, e.id);
            boolean dejaConfirme = e.ramasse || collectesPartageesConfirmees.contains(cle)
                    || collectesLocalesConfirmees.contains(cle);
            ancien.definirActif(!(dejaConfirme || collectesOptimistes.contains(cle)));
        }
        inventaireRecette.definirEtatPartage(requises, quantites);
        recalculerInventairePartage();
        mondeIngredientsPret = true;
        recettePartageeComplete = inventaireRecette.estComplete();
    }
    private void recalculerInventairePartage() {
        java.util.Map<TypeIngredient,Integer> requises = inventaireRecette.getQuantitesRequises();
        java.util.Map<TypeIngredient,Integer> ramassees = new java.util.EnumMap<>(TypeIngredient.class);
        for (int id = 0; id < ingredients.size(); id++) {
            String cle = cleCollecte(numeroNiveau, id);
            if (collectesOptimistes.contains(cle) || collectesLocalesConfirmees.contains(cle)
                    || collectesPartageesConfirmees.contains(cle)) {
                TypeIngredient type = ingredients.get(id).getTypeIngredient();
                ramassees.put(type, ramassees.getOrDefault(type, 0) + 1);
            }
        }
        inventaireRecette.definirEtatPartage(requises, ramassees);
        recettePartageeComplete = inventaireRecette.estComplete();
    }
    public java.util.Queue<String> getLeviersEnAttente() { return leviersEnAttente; }
    public java.util.Queue<String> getDegatsEnAttente() { return degatsEnAttente; }
    public List<Levier> getLeviers() { return carte.getLeviers(); }
    public void appliquerLeviersDistants(java.util.Map<Integer,Boolean> etats) {
        for (Levier l:carte.getLeviers()) if(etats.containsKey(l.getIndexMulti())) l.definirActif(etats.get(l.getIndexMulti()));
    }
    public List<Entite> getEntites() { return Collections.unmodifiableList(entites); }
    public void appliquerCollectesDistantes(List<Integer> indices) {
        for (Integer i : indices) { carte.retirerIngredient(i); if (i >= 0 && i < ingredients.size()) ingredients.get(i).definirActif(false); }
    }
    public void appliquerMonstres(List<GestionBDD.EtatMonstre> etats) {
        for (Entite entite : entites) if (entite instanceof Monstre) {
            Monstre m=(Monstre)entite;
            for (GestionBDD.EtatMonstre e:etats) if(e.id==m.getIndexMulti()) m.appliquerEtatDistant(e.x,e.y,e.pv,e.vivant,e.gauche);
        }
    }
    /** API à appeler depuis un système d'attaque; aucune attaque joueur n'existe actuellement. */
    public void demanderDegatsMonstre(int index, int degats) {
        if (infosPartie != null && infosPartie.estMultijoueur() && degats > 0) degatsEnAttente.add(index+":"+degats);
    }

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
    public Joueur getJoueur() {
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
    public int getNumeroNiveau() {
        return numeroNiveau;
    }
}
