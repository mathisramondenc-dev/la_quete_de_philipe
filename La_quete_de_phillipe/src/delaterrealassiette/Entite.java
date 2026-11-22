package delaterrealassiette;

/* ________________________________________________________________
 * ACTION : Centralise la position et les déplacements des entités.
 * [INFO] L'origine de la hitbox est le coin inférieur gauche.
 * [DEPENDANCE] La map est créée par Jeu et fournit collisionHitbox.
 * ________________________________________________________________ */
public abstract class Entite extends ElementDeJeu {
    protected int vitesseX;
    protected int vitesseY;
    protected int gravite = -6;
    protected int vitesseYMin = -24;
    protected final Map map;
    protected boolean monterDemande;
    protected boolean descendreDemande;
    protected boolean estSurEchelle;

    /* ________________________________________________________________
     * ACTION : Initialise la position et la map de l'entité.
     * [INFO] La map n'est jamais recréée par l'entité.
     * [DEPENDANCE] Jeu transmet son unique instance de Map.
     * ________________________________________________________________ */
    public Entite(Map map, int positionX, int positionY) {
        super(positionX, positionY);
        this.map = map;
    }

    /* ________________________________________________________________
     * ACTION : Initialise l'entité à l'origine.
     * [DEPENDANCE] Jeu transmet son unique instance de Map.
     * ________________________________________________________________ */
    public Entite(Map map) {
        this(map, 0, 0);
    }

    /* ________________________________________________________________
     * ACTION : Retourne la taille de tuile fournie par la carte.
     * [DEPENDANCE] Map fournit la taille des tuiles du niveau.
     * ________________________________________________________________ */
    @Override
    protected int getTailleTuileDessin() {
        return map.getTailleTuile();
    }

    /* ________________________________________________________________
     * ACTION : Applique les déplacements sur les deux axes.
     * ________________________________________________________________ */
    public void deplacer() {
        deplacerX();
        deplacerY();
    }

    /* ________________________________________________________________
     * ACTION : Déplace l'entité sur l'axe x pixel par pixel.
     * [INFO] Une collision annule la vitesse sur l'axe x.
     * [DEPENDANCE] La map fournit collisionHitbox.
     * ________________________________________________________________ */
    protected void deplacerX() {
        int[] dimensionsHitbox = getHitbox();
        int largeurHitbox = dimensionsHitbox[0];
        int hauteurHitbox = dimensionsHitbox[1];
        int direction = Integer.signum(vitesseX);
        int distance = Math.abs(vitesseX);

        for (int pixel = 0; pixel < distance; pixel++) {
            int prochainePositionX = positionX + direction;

            if (collisionPourEntite(
                    prochainePositionX, positionY, largeurHitbox, hauteurHitbox)) {
                boolean pousse = false;
                if (this instanceof Joueur
                        && "Etchebest".equals(((Joueur) this).getNomAvatarActuel())) {
                    for (Caisse caisse : map.getCaisses()) {
                        if (caisse.chevaucheRectangle(prochainePositionX, positionY,
                                largeurHitbox, hauteurHitbox)) {
                            pousse = map.pousserCaisse(caisse, direction);
                            break;
                        }
                    }
                }
                if (!pousse || collisionPourEntite(
                        prochainePositionX, positionY, largeurHitbox, hauteurHitbox)) {
                    vitesseX = 0;
                    break;
                }
            }
            positionX = prochainePositionX;
        }
    }

    /* ________________________________________________________________
     * ACTION : Déplace l'entité sur l'axe y pixel par pixel.
     * [INFO] Une collision annule la vitesse sur l'axe y.
     * [DEPENDANCE] La map fournit collisionHitbox.
     * ________________________________________________________________ */
    protected void deplacerY() {
        int[] dimensionsHitbox = getHitbox();
        int largeurHitbox = dimensionsHitbox[0];
        int hauteurHitbox = dimensionsHitbox[1];
        int direction = Integer.signum(vitesseY);
        int distance = Math.abs(vitesseY);

        for (int pixel = 0; pixel < distance; pixel++) {
            int prochainePositionY = positionY + direction;

            if (collisionPourEntite(
                    positionX, prochainePositionY, largeurHitbox, hauteurHitbox)) {
                vitesseY = 0;
                break;
            }
            int sommetEchelle = map.getSommetEchelleFranchi(positionX, positionY,
                    prochainePositionY, largeurHitbox);
            if (!descendreDemande && sommetEchelle != Integer.MIN_VALUE) {
                prochainePositionY = sommetEchelle;
                vitesseY = 0;
            }
            positionY = prochainePositionY;
        }
    }

    /* ________________________________________________________________
     * ACTION : Ignore la caisse courante lors de ses propres déplacements.
     * ________________________________________________________________ */
    private boolean collisionPourEntite(int x, int y, int largeurHitbox, int hauteurHitbox) {
        if (this instanceof Caisse) {
            return map.collisionHitboxSansCaisses(x, y, largeurHitbox, hauteurHitbox,
                    (Caisse) this);
        }
        return map.collisionHitbox(x, y, largeurHitbox, hauteurHitbox);
    }

    /* ________________________________________________________________
     * ACTION : Détecte le sol partagé par les joueurs, monstres et caisses.
     * ________________________________________________________________ */
    protected boolean estAuSol() {
        int[] dimensionsHitbox = getHitbox();
        return map.aUnSolSous(positionX, positionY,
                dimensionsHitbox[0], dimensionsHitbox[1]) && vitesseY <= 0;
    }

    /* ________________________________________________________________
     * ACTION : Enregistre les commandes verticales de déplacement sur échelle.
     * ________________________________________________________________ */
    public void definirCommandesEchelle(boolean monter, boolean descendre) {
        monterDemande = monter;
        descendreDemande = descendre;
    }

    /* ________________________________________________________________
     * ACTION : Met à jour la gravité et le déplacement vertical sur échelle.
     * ________________________________________________________________ */
    protected void mettreAJourGraviteEtEchelle(boolean demandeSaut) {
        int[] dimensionsHitbox = getHitbox();
        estSurEchelle = map.estSurOuDansEchelle(positionX, positionY,
                dimensionsHitbox[0], dimensionsHitbox[1]);
        if (estSurEchelle && monterDemande && !demandeSaut) {
            vitesseY = getVitesse();
            return;
        }
        if (estSurEchelle && descendreDemande && !demandeSaut) {
            vitesseY = -getVitesse();
            return;
        }
        if (demandeSaut) {
            estSurEchelle = false;
            return;
        }
        if (estSurEchelle) {
            vitesseY = 0;
            return;
        }
        if (estAuSol()) {
            vitesseY = 0;
        } else {
            vitesseY = Math.max(vitesseY + gravite, vitesseYMin);
        }
    }

    /* ________________________________________________________________
     * ACTION : Retourne la vitesse de déplacement.
     * ________________________________________________________________ */
    public abstract int getVitesse();

    /* ________________________________________________________________
     * ACTION : Retourne l'impulsion du saut.
     * ________________________________________________________________ */
    public abstract int getForceSaut();

    /* ________________________________________________________________
     * ACTION : Actualise les intentions et les déplacements.
     * ________________________________________________________________ */
    public abstract void mettreAJour();

}
