package delaterrealassiette;

/**
 *
 * @author Antoine
 */

public class Joueur extends Entite {
    //--Description IHM--
    public static final int BtnHaut = 0;
    public static final int BtnBas = 1;
    public static final int BtnGauche = 2;
    public static final int BtnDroite = 3;
    public static final int BtnEtchebest = 4;
    public static final int BtnLignac = 5;
    public static final int BtnTarayre = 6;
    public static final int BtnCompetence = 7; //clic gauche souris
    public static final int BtnSauter = 8;

    // --- Données du joueur (à synchroniser avec la BDD SQL) ---
    private final String pseudo;
    private final int maxPv;
    private int pv;
    private int score;
    
    // --- Gestion des chefs ---
    private final Avatar etchebest;
    private final Avatar lignac;
    private final Avatar tarayre;
    
    // Chef actuellement contrôlé
    private Avatar avatarActuel;
    
    //--Gestion du controle--
    private boolean allerHaut;
    private boolean allerBas;
    private boolean allerGauche;
    private boolean allerDroite;
    private boolean sauter;
    
    private boolean utiliserCompetence;
    
    private boolean devenirEtchebest;
    private boolean devenirLignac;
    private boolean devenirTarayre;

    /**
     * Constructeur du Joueur (Appelé depuis la FenetreMenu).
     * On passe (0, 0) à Entite via super car la carte n'est pas encore chargée.
     */
    public Joueur(String pseudo) {
        super(0, 0); 
        
        this.pseudo = pseudo;
        this.maxPv = 100;
        this.pv = this.maxPv; // On commence avec tous les PV
        this.score = 0;
        
        // Initialisation des 3 avatars
        this.etchebest = new Avatar("Etchebest", 5, 12, new int[]{32, 64}, "/sprites/etchebest.png");
        this.lignac = new Avatar("Lignac", 7, 15, new int[]{28, 60}, "/sprites/lignac.png");
        this.tarayre = new Avatar("Tarayre", 6, 10, new int[]{30, 58}, "/sprites/tarayre.png");
        
        // Le joueur commence la partie avec Etchebest par défaut
        this.avatarActuel = this.etchebest;
        
        //controles a 0
        this.allerHaut=false;
        this.allerBas=false;
        this.allerGauche=false;
        this.allerDroite=false;
        this.sauter=false;
        this.utiliserCompetence=false;

        this.devenirEtchebest=false;
        this.devenirLignac=false;
        this.devenirTarayre=false;
    }

    /**
     * Change le chef actuellement contrôlé par le joueur.
     */
    public void switchChef(Avatar nouvelAvatar) {
        this.avatarActuel = nouvelAvatar;
    }

    // Getters pour permettre le switch vers les autres avatars
    public Avatar getEtchebest() { return etchebest; }
    public Avatar getLignac() { return lignac; }
    public Avatar getTarayre() { return tarayre; }

    // ==========================================================
    // MÉTHODES REDÉFINIES DE LA CLASSE ENTITE 
    // ==========================================================

    @Override
    public int getVitesse() {
        return avatarActuel.getVitesse();
    }

    @Override
    public int getForceSaut() {
        return avatarActuel.getForceSaut();
    }

    @Override
    public int[] getHitbox() {
        return avatarActuel.getHitbox();
    }

    @Override
    public String getCheminSprite() {
        return avatarActuel.getCheminSprite();
    }

    @Override
    public void mettreAJour() {
        // TODO : Gestion des touches du clavier et gravité
    }

    // ==========================================================
    // GETTERS & SETTERS (Multijoueur et BDD)
    // ==========================================================
    
    public String getNomAvatarActuel() {
        return (avatarActuel != null) ? avatarActuel.getNom() : "";
    }

    public String getPseudo() {
        return pseudo;
    }

    public int getMaxPv() {
        return maxPv;
    }

    public int getPv() {
        return pv;
    }
    
    /**
     * Modifie les PV du joueur de manière sécurisée.
     */
    public void modifierPv(int montant) {
        this.pv += montant;
        
        if (this.pv <= 0) {
            this.pv = 0;
        } else if (this.pv > this.maxPv) {
            this.pv = this.maxPv;
        }
    }

    /**
     * Retourne true si le joueur n'a plus de points de vie.
     */
    public boolean estMort() {
        return this.pv <= 0;
    }

    public int getScore() {
        return score;
    }
    
    public void ajouterScore(int points) {
        this.score += points;
    }
    
    public void miseAJourIntentions(boolean[] etatTouches) {
        // Transfert direct et brutal des états, sans aucun test conditionnel
        this.allerHaut = etatTouches[BtnHaut];
        this.allerBas = etatTouches[BtnBas];
        this.allerGauche = etatTouches[BtnGauche];
        this.allerDroite = etatTouches[BtnDroite];
        this.sauter = etatTouches[BtnSauter];
        
        this.utiliserCompetence = etatTouches[BtnCompetence];
        
        this.devenirEtchebest = etatTouches[BtnEtchebest];
        this.devenirLignac = etatTouches[BtnLignac];
        this.devenirTarayre = etatTouches[BtnTarayre];
    }
}