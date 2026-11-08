package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Gère les avatars, les intentions et les données du joueur.
 * [INFO] Les ordonnées augmentent vers le haut.
 * [DEPENDANCE] Entite conserve la map partagée et résout les déplacements.
 * ________________________________________________________________ */
public class Joueur extends Entite {
    public static final int toucheHaut = 0;
    public static final int toucheBas = 1;
    public static final int toucheGauche = 2;
    public static final int toucheDroite = 3;
    public static final int toucheEtchebest = 4;
    public static final int toucheLignac = 5;
    public static final int toucheTarayre = 6;
    public static final int toucheCompetence = 7;
    public static final int toucheSauter = 8;

    private final String pseudo;
    private final int pointsDeVieMax;
    private int pointsDeVie;
    private int score;

    private final Avatar etchebest;
    private final Avatar lignac;
    private final Avatar tarayre;
    private Avatar avatarActuel;

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

    /* ________________________________________________________________
     * ACTION : Initialise le joueur et ses trois avatars.
     * [DEPENDANCE] Jeu transmet son unique instance de Map.
     * ________________________________________________________________ */
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

        avatarActuel = etchebest;
        chargerImage(avatarActuel.getCheminImage());
    }

    /* ________________________________________________________________
     * ACTION : Recrée le joueur sur une nouvelle carte en conservant son état.
     * [INFO] Les vitesses et les intentions repartent à zéro.
     * ________________________________________________________________ */
    public Joueur(Map nouvelleCarte, Joueur joueurPrecedent) {
        this(nouvelleCarte, joueurPrecedent.pseudo);
        avatarActuel = joueurPrecedent.avatarActuel;
        chargerImage(avatarActuel.getCheminImage());
        pointsDeVie = joueurPrecedent.pointsDeVie;
        score = joueurPrecedent.score;
    }

    /* ________________________________________________________________
     * ACTION : Change l'avatar contrôlé si sa hitbox peut tenir sur la carte.
     * [INFO] Les pieds restent à la même hauteur et la hitbox reste centrée.
     * [DEPENDANCE] Map vérifie les tuiles solides et les portes fermées.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Retourne l'avatar actuellement contrôlé.
     * ________________________________________________________________ */
    public Avatar getAvatarActuel() {
        return avatarActuel;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le profil Etchebest.
     * ________________________________________________________________ */
    public Avatar getEtchebest() {
        return etchebest;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le profil Lignac.
     * ________________________________________________________________ */
    public Avatar getLignac() {
        return lignac;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le profil Tarayre.
     * ________________________________________________________________ */
    public Avatar getTarayre() {
        return tarayre;
    }

    /* ________________________________________________________________
     * ACTION : Retourne la vitesse de l'avatar actif.
     * ________________________________________________________________ */
    @Override
    public int getVitesse() {
        return avatarActuel.getVitesseDeplacementX();
    }

    /* ________________________________________________________________
     * ACTION : Retourne l'impulsion de l'avatar actif.
     * ________________________________________________________________ */
    @Override
    public int getForceSaut() {
        return avatarActuel.getForceSaut();
    }

    /* ________________________________________________________________
     * ACTION : Retourne la hitbox de l'avatar actif.
     * ________________________________________________________________ */
    @Override
    public int[] getHitbox() {
        return avatarActuel.getHitbox();
    }

    /* ________________________________________________________________
     * ACTION : Retourne le chemin de l'image de l'avatar actif.
     * ________________________________________________________________ */
    @Override
    public String getCheminImage() {
        return avatarActuel.getCheminImage();
    }

    /* ________________________________________________________________
     * ACTION : Applique les intentions, la gravité et le saut.
     * [INFO] L'origine de la hitbox est le coin inférieur gauche.
     * [DEPENDANCE] La map détecte les collisions et Entite résout les déplacements.
     * ________________________________________________________________ */
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

        int[] hitbox = getHitbox();
        boolean estAuSol = map.collisionHitbox(
                positionX, positionY - 1, hitbox[0], hitbox[1])
                && vitesseY <= 0;

        if (estAuSol) {
            vitesseY = 0;
        } else {
            vitesseY = Math.max(vitesseY + gravite, vitesseYMin);
        }

        if (sautDemande && estAuSol) {
            vitesseY = getForceSaut();
        }
        sautDemande = false;

        deplacer();
    }

    /* ________________________________________________________________
     * ACTION : Enregistre les intentions issues des touches.
     * [INFO] Le saut est demandé uniquement au nouvel appui.
     * ________________________________________________________________ */
    public void miseAJourIntentions(boolean[] touchesAppuyees) {
        allerGauche = touchesAppuyees[toucheGauche];
        allerDroite = touchesAppuyees[toucheDroite];

        boolean toucheSautAppuyee = touchesAppuyees[toucheSauter];
        if (toucheSautAppuyee && !toucheSautPrecedemmentAppuyee) {
            sautDemande = true;
        }
        toucheSautPrecedemmentAppuyee = toucheSautAppuyee;

        utiliserCompetence = touchesAppuyees[toucheCompetence];
        if (utiliserCompetence && !toucheCompetencePrecedemmentAppuyee
                && "Tarayre".equals(avatarActuel.getNom())) {
            activationLevierDemandee = true;
        }
        toucheCompetencePrecedemmentAppuyee = utiliserCompetence;
        devenirEtchebest = touchesAppuyees[toucheEtchebest];
        devenirLignac = touchesAppuyees[toucheLignac];
        devenirTarayre = touchesAppuyees[toucheTarayre];
    }

    /* ________________________________________________________________
     * ACTION : Consomme la demande d'activation du levier.
     * [INFO] Seul Tarayre transforme un nouvel appui sur E en demande.
     * ________________________________________________________________ */
    public boolean consommerActivationLevier() {
        boolean demandee = activationLevierDemandee;
        activationLevierDemandee = false;
        return demandee;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le nom de l'avatar actif.
     * ________________________________________________________________ */
    public String getNomAvatarActuel() {
        return avatarActuel.getNom();
    }

    /* ________________________________________________________________
     * ACTION : Retourne le pseudo du joueur.
     * ________________________________________________________________ */
    public String getPseudo() {
        return pseudo;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le maximum de points de vie.
     * ________________________________________________________________ */
    public int getPointsDeVieMax() {
        return pointsDeVieMax;
    }

    /* ________________________________________________________________
     * ACTION : Retourne les points de vie actuels.
     * ________________________________________________________________ */
    public int getPointsDeVie() {
        return pointsDeVie;
    }

    /* ________________________________________________________________
     * ACTION : Modifie les points de vie dans leurs limites.
     * ________________________________________________________________ */
    public void modifierPointsDeVie(int montant) {
        pointsDeVie = Math.max(0,
                Math.min(pointsDeVieMax, pointsDeVie + montant));
    }

    /* ________________________________________________________________
     * ACTION : Indique si le joueur est mort.
     * ________________________________________________________________ */
    public boolean estMort() {
        return pointsDeVie <= 0;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le score du joueur.
     * ________________________________________________________________ */
    public int getScore() {
        return score;
    }

    /* ________________________________________________________________
     * ACTION : Ajoute des points au score.
     * ________________________________________________________________ */
    public void ajouterScore(int points) {
        score += points;
    }
}