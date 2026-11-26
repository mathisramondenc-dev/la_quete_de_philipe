package delaterrealassiette;

/**
 * Personnage contrôlé par le joueur local.
 *
 * Le joueur possède quatre chefs (Avatar) et en incarne un à la fois : la
 * vitesse, la force de saut, la hitbox et le sprite sont lus sur l'avatar
 * actif, ce qui permet de changer de chef sans recréer l'objet. Pseudo, points
 * de vie, score et recharge du soin appartiennent au joueur, pas à l'avatar.
 *
 * Stratégie d'entrée : le clavier n'agit pas directement sur le personnage.
 * miseAJourIntentions() traduit l'état des touches en « intentions » (aller à
 * gauche, sauter, devenir Lignac...), puis mettreAJour() les applique pendant
 * le tick. Les actions ponctuelles (saut, levier, soin) ne se déclenchent qu'à
 * l'appui (front montant), pas tant que la touche reste enfoncée.
 *
 * Compétences : Etchebest pousse les caisses (voir Entite), Tarayre active
 * les leviers, Maite soigne avec un temps de recharge.
 */
public class Joueur extends Entite {
    /** Index des touches dans le tableau fourni par ClavierSouris. */
    public static final int toucheHaut = 0;
    public static final int toucheBas = 1;
    public static final int toucheGauche = 2;
    public static final int toucheDroite = 3;
    public static final int toucheEtchebest = 4;
    public static final int toucheLignac = 5;
    public static final int toucheTarayre = 6;
    public static final int toucheCompetence = 7;
    public static final int toucheSauter = 8;
    public static final int toucheMaite = 9;

    private final String pseudo;
    private final int pointsDeVieMax;
    private int pointsDeVie;
    private int score;

    private final Avatar etchebest;
    private final Avatar lignac;
    private final Avatar tarayre;
    private final Avatar maite;
    private Avatar avatarActuel;

    // Intentions calculées depuis le clavier, consommées pendant le tick
    private boolean allerGauche;
    private boolean allerDroite;
    private boolean sautDemande;
    private boolean toucheSautPrecedemmentAppuyee;
    private boolean toucheCompetencePrecedemmentAppuyee;
    private boolean activationLevierDemandee;
    private boolean utiliserCompetence;
    private boolean devenirEtchebest;
    private boolean devenirLignac;
    private boolean devenirTarayre;
    private boolean devenirMaite;
    private boolean soinDemande;
    private int tempsRechargeSoin;
    /** Durée de recharge du soin de Maite, en ticks. */
    private static final int dureeRechargeSoin = 375;

    /**
     * Crée le joueur avec ses quatre chefs ; Etchebest est incarné au départ.
     * Caractéristiques : (vitesse, force de saut, hitbox largeur x hauteur).
     */
    public Joueur(Map map, String pseudo) {
        super(map, 0, 0);
        this.pseudo = pseudo;
        pointsDeVieMax = 100;
        pointsDeVie = pointsDeVieMax;

        etchebest = new Avatar("Etchebest", 6, 30,
                new int[]{32, 64}, "images/sprites/etchebest.png");
        lignac = new Avatar("Lignac", 10, 40,
                new int[]{28, 60}, "images/sprites/lignac.png");
        tarayre = new Avatar("Tarayre", 8, 30,
                new int[]{18, 30}, "images/sprites/tarayre.png");
        maite = new Avatar("Maite", 6, 30,
                new int[]{32, 64}, "images/sprites/maite.png");

        avatarActuel = etchebest;
        chargerImage(avatarActuel.getCheminImage());
    }

    /**
     * Copie le joueur sur la carte d'un nouveau niveau : chef, PV, score et
     * recharge sont conservés, vitesses et intentions repartent de zéro.
     */
    public Joueur(Map nouvelleCarte, Joueur joueurPrecedent) {
        this(nouvelleCarte, joueurPrecedent.pseudo);
        avatarActuel = joueurPrecedent.avatarActuel;
        chargerImage(avatarActuel.getCheminImage());
        pointsDeVie = joueurPrecedent.pointsDeVie;
        score = joueurPrecedent.score;
        tempsRechargeSoin = joueurPrecedent.tempsRechargeSoin;
    }

    /**
     * Change de chef si la nouvelle hitbox tient à l'endroit actuel.
     * Les pieds restent à la même hauteur et le personnage reste centré ;
     * si le nouveau chef rentrerait dans un mur, le changement est refusé.
     */
    public void changerChef(Avatar nouvelAvatar) {
        if (avatarActuel == nouvelAvatar) {
            return;
        }

        int[] hitboxActuelle = avatarActuel.getHitbox();
        int[] nouvelleHitbox = nouvelAvatar.getHitbox();
        int nouvellePositionX = positionX
                + (hitboxActuelle[0] - nouvelleHitbox[0]) / 2;

        if (map.collisionHitbox(nouvellePositionX, positionY,
                nouvelleHitbox[0], nouvelleHitbox[1])) {
            return;
        }

        positionX = nouvellePositionX;
        avatarActuel = nouvelAvatar;
        chargerImage(avatarActuel.getCheminImage());
    }

    // ----- Accès aux chefs -----

    public Avatar getAvatarActuel() {
        return avatarActuel;
    }

    public Avatar getEtchebest() {
        return etchebest;
    }

    public Avatar getLignac() {
        return lignac;
    }

    public Avatar getTarayre() {
        return tarayre;
    }

    public Avatar getMaite() {
        return maite;
    }

    // ----- Caractéristiques déléguées à l'avatar actif -----

    @Override
    public int getVitesse() {
        return avatarActuel.getVitesseDeplacementX();
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
    public String getCheminImage() {
        return avatarActuel.getCheminImage();
    }

    /**
     * Tick du joueur : changement de chef, vitesse horizontale et orientation,
     * saut (autorisé au sol ou sur une échelle), gravité, puis déplacement.
     * La force de saut est réappliquée après la gravité pour que celle-ci ne
     * l'écrase pas.
     */
    @Override
    public void mettreAJour() {
        if (devenirEtchebest) {
            changerChef(etchebest);
        }
        if (devenirLignac) {
            changerChef(lignac);
        }
        if (devenirTarayre) {
            changerChef(tarayre);
        }
        if (devenirMaite) {
            changerChef(maite);
        }

        vitesseX = 0;

        if (allerGauche) {
            vitesseX -= getVitesse();
        }
        if (allerDroite) {
            vitesseX += getVitesse();
        }
        if (vitesseX < 0) {
            definirOrientationGauche(true);
        } else if (vitesseX > 0) {
            definirOrientationGauche(false);
        }

        boolean estAuSol = estAuSol();
        boolean sautPossible = estAuSol || estSurEchelle;
        if (sautDemande && sautPossible) {
            vitesseY = getForceSaut();
            estSurEchelle = false;
        }
        mettreAJourGraviteEtEchelle(sautDemande);
        if (sautDemande && sautPossible) {
            vitesseY = getForceSaut();
        }
        // Permet de descendre une échelle quand on se tient sur son sommet
        if (descendreDemande && estSurEchelle && estAuSol) {
            vitesseY = -getVitesse();
        }
        sautDemande = false;
        deplacer();
    }

    /**
     * Traduit l'état du clavier en intentions pour le prochain tick.
     * Appelée une fois par tick ; c'est aussi elle qui décompte la recharge
     * du soin.
     *
     * @param touchesAppuyees état des touches, indexé par les constantes touche*
     */
    public void miseAJourIntentions(boolean[] touchesAppuyees) {
        allerGauche = touchesAppuyees[toucheGauche];
        allerDroite = touchesAppuyees[toucheDroite];
        definirCommandesEchelle(touchesAppuyees[toucheHaut], touchesAppuyees[toucheBas]);

        boolean toucheSautAppuyee = touchesAppuyees[toucheSauter];
        if (toucheSautAppuyee && !toucheSautPrecedemmentAppuyee) {
            sautDemande = true;
        }
        toucheSautPrecedemmentAppuyee = toucheSautAppuyee;

        if (tempsRechargeSoin > 0) {
            tempsRechargeSoin--;
        }

        // La touche de compétence a un effet différent selon le chef incarné
        utiliserCompetence = touchesAppuyees[toucheCompetence];
        if (utiliserCompetence && !toucheCompetencePrecedemmentAppuyee
                && "Tarayre".equals(avatarActuel.getNom())) {
            activationLevierDemandee = true;
        }
        if (utiliserCompetence && !toucheCompetencePrecedemmentAppuyee
                && "Maite".equals(avatarActuel.getNom())
                && tempsRechargeSoin == 0) {
            soinDemande = true;
        }
        toucheCompetencePrecedemmentAppuyee = utiliserCompetence;
        devenirEtchebest = touchesAppuyees[toucheEtchebest];
        devenirLignac = touchesAppuyees[toucheLignac];
        devenirTarayre = touchesAppuyees[toucheTarayre];
        devenirMaite = touchesAppuyees[toucheMaite];
    }

    /**
     * Lit puis efface la demande d'activation de levier (Tarayre).
     * Le principe « lire et effacer » garantit qu'un appui n'agit qu'une fois.
     *
     * @return true si une activation était en attente
     */
    public boolean consommerActivationLevier() {
        boolean demandee = activationLevierDemandee;
        activationLevierDemandee = false;
        return demandee;
    }

    /**
     * Lit puis efface la demande de soin (Maite) et lance la recharge.
     *
     * @return true si un soin doit être appliqué
     */
    public boolean consommerSoinDemande() {
        boolean demandee = soinDemande;
        soinDemande = false;
        if (demandee) {
            tempsRechargeSoin = dureeRechargeSoin;
        }
        return demandee;
    }

    /** @return avancement de la recharge du soin, de 0.0 (vient d'être utilisé) à 1.0 (prêt) */
    public double getProgressionRechargeSoin() {
        return 1.0 - (double) tempsRechargeSoin / dureeRechargeSoin;
    }

    // ----- Identité, vie et score -----

    public String getNomAvatarActuel() {
        return avatarActuel.getNom();
    }

    public String getPseudo() {
        return pseudo;
    }

    public int getPointsDeVieMax() {
        return pointsDeVieMax;
    }

    public int getPointsDeVie() {
        return pointsDeVie;
    }

    /**
     * Ajoute (soin) ou retire (dégâts) des points de vie, bornés entre 0 et le maximum.
     *
     * @param montant valeur positive ou négative
     */
    public void modifierPointsDeVie(int montant) {
        pointsDeVie = Math.max(0,
                Math.min(pointsDeVieMax, pointsDeVie + montant));
    }

    public boolean estMort() {
        return pointsDeVie <= 0;
    }

    public int getScore() {
        return score;
    }

    public void ajouterScore(int points) {
        score += points;
    }
}
