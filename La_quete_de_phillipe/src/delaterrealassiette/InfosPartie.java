// InfosPartie.java
package delaterrealassiette;

import java.util.Random;

/* ________________________________________________________________
 * ACTION : Regroupe les informations saisies dans les fenêtres
 *          "Héberger" et "Rejoindre" (pseudo, identifiant de la partie,
 *          mot de passe, nombre de joueurs, niveau).
 * [INFO] Contient aussi les règles de vérification des champs.
 * [DEPENDANCE] FenetreHeberger et FenetreRejoindre la créent, puis
 *              FenetreDeJeu la reçoit. Plus tard : GestionBDD pourra
 *              l'utiliser pour enregistrer ou vérifier la partie.
 * ________________________________________________________________ */
public class InfosPartie {

    public static final int NB_JOUEURS_MIN = 2;
    public static final int NB_JOUEURS_MAX = 4;
    public static final int LONGUEUR_IDENTIFIANT = 6;
    public static final int PSEUDO_LONGUEUR_MIN = 3;
    public static final int PSEUDO_LONGUEUR_MAX = 15;

    // Lettres et chiffres faciles à lire (pas de O/0 ni de I/1)
    private static final String CARACTERES_IDENTIFIANT = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final boolean hote;
    private final String pseudo;
    private final String identifiantPartie;
    private final String motDePasse;
    private final int nbJoueursMax;
    private final String fichierNiveau;

    /* ________________________________________________________________
     * ACTION : Constructeur privé, utiliser pourHote() ou pourInvite().
     * ________________________________________________________________ */
    private InfosPartie(boolean hote, String pseudo, String identifiantPartie,
            String motDePasse, int nbJoueursMax, String fichierNiveau) {
        this.hote = hote;
        this.pseudo = pseudo;
        this.identifiantPartie = identifiantPartie;
        this.motDePasse = motDePasse;
        this.nbJoueursMax = nbJoueursMax;
        this.fichierNiveau = fichierNiveau;
    }

    /* ________________________________________________________________
     * ACTION : Crée les informations d'un joueur qui héberge la partie.
     * ________________________________________________________________ */
    public static InfosPartie pourHote(String pseudo, String identifiantPartie,
            String motDePasse, int nbJoueursMax, String fichierNiveau) {
        return new InfosPartie(true, pseudo, identifiantPartie,
                motDePasse, nbJoueursMax, fichierNiveau);
    }

    /* ________________________________________________________________
     * ACTION : Crée les informations d'un joueur qui rejoint une partie.
     * [INFO] Le nombre de joueurs et le niveau sont choisis par l'hôte :
     *        ils seront à lire dans la BDD (0 et null en attendant).
     * ________________________________________________________________ */
    public static InfosPartie pourInvite(String pseudo, String identifiantPartie,
            String motDePasse) {
        return new InfosPartie(false, pseudo, identifiantPartie,
                motDePasse, 0, null);
    }

    // ===================== Vérifications =====================

    /* ________________________________________________________________
     * ACTION : Vérifie un pseudo.
     * [INFO] Retourne null si le pseudo est valide, sinon le message d'erreur.
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
     * [INFO] Retourne null si l'identifiant est valide, sinon le message d'erreur.
     * ________________________________________________________________ */
    public static String verifierIdentifiant(String identifiant) {
        if (identifiant == null || identifiant.trim().isEmpty()) {
            return "L'identifiant de la partie est obligatoire.";
        }
        if (identifiant.length() != LONGUEUR_IDENTIFIANT
                || !identifiant.matches("[A-Z0-9]+")) {
            return "L'identifiant de la partie fait " + LONGUEUR_IDENTIFIANT
                    + " caractères (lettres et chiffres), par exemple K7PX3M.";
        }
        return null;
    }

    /* ________________________________________________________________
     * ACTION : Génère un identifiant de partie aléatoire (ex : K7PX3M).
     * [INFO] L'hôte le donne aux autres joueurs pour qu'ils rejoignent.
     * ________________________________________________________________ */
    public static String genererIdentifiant() {
        Random hasard = new Random();
        StringBuilder identifiant = new StringBuilder();
        for (int i = 0; i < LONGUEUR_IDENTIFIANT; i++) {
            int indice = hasard.nextInt(CARACTERES_IDENTIFIANT.length());
            identifiant.append(CARACTERES_IDENTIFIANT.charAt(indice));
        }
        return identifiant.toString();
    }

    // ===================== Accesseurs =====================

    public boolean estHote() {
        return hote;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getIdentifiantPartie() {
        return identifiantPartie;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le mot de passe de la partie ("" si aucun).
     * ________________________________________________________________ */
    public String getMotDePasse() {
        return motDePasse;
    }

    public boolean aUnMotDePasse() {
        return motDePasse != null && !motDePasse.isEmpty();
    }

    public int getNbJoueursMax() {
        return nbJoueursMax;
    }

    public String getFichierNiveau() {
        return fichierNiveau;
    }

    @Override
    public String toString() {
        return (hote ? "Hôte" : "Invité") + " " + pseudo
                + " - partie " + identifiantPartie
                + (aUnMotDePasse() ? " (protégée)" : "")
                + (hote ? " - " + nbJoueursMax + " joueurs max - " + fichierNiveau : "");
    }
}
