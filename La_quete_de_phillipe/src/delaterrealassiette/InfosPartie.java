package delaterrealassiette;

/**
 * Informations de connexion d'un joueur à une partie multijoueur : pseudo,
 * code de la partie, mot de passe et identifiant de session.
 *
 * Objet immuable, créé par GestionBDD une fois l'entrée dans la partie
 * acceptée, puis transmis à la fenêtre de jeu et à la synchronisation.
 * L'identifiant de session (unique pour chaque instance du jeu lancée) est
 * ce qui distingue ce joueur dans la base.
 *
 * La classe regroupe aussi les règles de validation des champs saisis, pour
 * que tous les formulaires appliquent exactement les mêmes. Chaque méthode
 * verifier...() renvoie null si la valeur est correcte, sinon le message
 * d'erreur à afficher.
 */
public class InfosPartie {

    public static final int PSEUDO_LONGUEUR_MIN = 3;
    public static final int PSEUDO_LONGUEUR_MAX = 15;
    public static final int IDENTIFIANT_LONGUEUR_MAX = 20;
    public static final int MOT_DE_PASSE_LONGUEUR_MIN = 4;

    private final String pseudo;
    private final String identifiantPartie;
    private final String motDePasse;
    private final String sessionId;

    /** Informations sans session (hors base de données). Les champs doivent déjà être vérifiés. */
    public InfosPartie(String pseudo, String identifiantPartie, String motDePasse) {
        this(pseudo, identifiantPartie, motDePasse, null);
    }

    /** @param sessionId identifiant de ce joueur dans la base, attribué par GestionBDD */
    public InfosPartie(String pseudo, String identifiantPartie, String motDePasse, String sessionId) {
        this.pseudo = pseudo;
        this.identifiantPartie = identifiantPartie;
        this.motDePasse = motDePasse;
        this.sessionId = sessionId;
    }

    // ----- Validation des champs (null = valide, sinon message d'erreur) -----

    /** Pseudo : 3 à 15 caractères, lettres sans accent, chiffres, _ et -. */
    public static String verifierPseudo(String pseudo) {
        if (pseudo == null || pseudo.trim().isEmpty()) {
            return "Le pseudo est obligatoire.";
        }
        if (pseudo.length() < PSEUDO_LONGUEUR_MIN || pseudo.length() > PSEUDO_LONGUEUR_MAX) {
            return "Le pseudo doit faire entre " + PSEUDO_LONGUEUR_MIN
                    + " et " + PSEUDO_LONGUEUR_MAX + " caractères.";
        }
        if (!pseudo.matches("[A-Za-z0-9_-]+")) {
            return "Le pseudo ne peut contenir que des lettres (sans accent), "
                    + "des chiffres, _ et -.";
        }
        return null;
    }

    /** Code de partie : 20 caractères au plus, mêmes caractères autorisés que le pseudo. */
    public static String verifierIdentifiant(String identifiant) {
        if (identifiant == null || identifiant.trim().isEmpty()) {
            return "L'identifiant de la partie est obligatoire.";
        }
        if (identifiant.length() > IDENTIFIANT_LONGUEUR_MAX) {
            return "L'identifiant de la partie fait au maximum "
                    + IDENTIFIANT_LONGUEUR_MAX + " caractères.";
        }
        if (!identifiant.matches("[A-Za-z0-9_-]+")) {
            return "L'identifiant de la partie ne peut contenir que des lettres "
                    + "(sans accent), des chiffres, _ et -.";
        }
        return null;
    }

    /** Mot de passe : obligatoire, 4 caractères au moins. */
    public static String verifierMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.isEmpty()) {
            return "Le mot de passe est obligatoire.";
        }
        if (motDePasse.length() < MOT_DE_PASSE_LONGUEUR_MIN) {
            return "Le mot de passe doit faire au moins "
                    + MOT_DE_PASSE_LONGUEUR_MIN + " caractères.";
        }
        return null;
    }

    // ----- Accesseurs -----

    public String getPseudo() {
        return pseudo;
    }

    public String getIdentifiantPartie() {
        return identifiantPartie;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public String getSessionId() { return sessionId; }

    /** Le mot de passe n'apparaît jamais dans le texte affiché. */
    @Override
    public String toString() {
        return pseudo + " - partie " + identifiantPartie;
    }
}
