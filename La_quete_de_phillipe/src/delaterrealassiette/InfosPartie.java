// InfosPartie.java
package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Regroupe les informations saisies dans la fenêtre
 *          "Se connecter à une partie" : pseudo, identifiant de la
 *          partie et mot de passe.
 * [INFO] Contient aussi les règles de vérification des champs.
 * [DEPENDANCE] FenetreConnexion la crée, puis FenetreDeJeu la reçoit.
 *              Plus tard : GestionBDD pourra l'utiliser pour vérifier
 *              la partie et le mot de passe.
 * ________________________________________________________________ */
public class InfosPartie {

    public static final int PSEUDO_LONGUEUR_MIN = 3;
    public static final int PSEUDO_LONGUEUR_MAX = 15;
    public static final int IDENTIFIANT_LONGUEUR_MAX = 20;
    public static final int MOT_DE_PASSE_LONGUEUR_MIN = 4;

    private final String pseudo;
    private final String identifiantPartie;
    private final String motDePasse;

    /* ________________________________________________________________
     * ACTION : Crée les informations de connexion d'un joueur.
     * [INFO] Les champs doivent avoir été vérifiés avant (verifier...).
     * ________________________________________________________________ */
    public InfosPartie(String pseudo, String identifiantPartie, String motDePasse) {
        this.pseudo = pseudo;
        this.identifiantPartie = identifiantPartie;
        this.motDePasse = motDePasse;
    }

    // ===================== Vérifications =====================
    // Chaque méthode retourne null si le champ est valide,
    // sinon le message d'erreur à afficher.

    /* ________________________________________________________________
     * ACTION : Vérifie un pseudo.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Vérifie un identifiant de partie.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Vérifie un mot de passe (obligatoire).
     * ________________________________________________________________ */
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

    // ===================== Accesseurs =====================

    public String getPseudo() {
        return pseudo;
    }

    public String getIdentifiantPartie() {
        return identifiantPartie;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    @Override
    public String toString() {
        // Le mot de passe n'est jamais affiché
        return pseudo + " - partie " + identifiantPartie;
    }
}
