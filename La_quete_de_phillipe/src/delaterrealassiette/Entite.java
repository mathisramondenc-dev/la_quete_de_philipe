package delaterrealassiette;

/**
 * Élément mobile soumis à la physique : joueurs, monstres et caisses.
 *
 * Stratégie de déplacement : à chaque tick, la vitesse est appliquée pixel
 * par pixel, d'abord en X puis en Y. Avant chaque pixel on interroge la Map
 * (collisionHitbox) ; au premier obstacle on s'arrête et la vitesse sur cet
 * axe est remise à 0. Séparer les axes permet de glisser le long d'un mur et
 * le pas d'un pixel empêche de traverser un bloc à grande vitesse.
 *
 * La gravité est une accélération constante plafonnée (vitesseYMin) ; elle
 * est suspendue sur une échelle, où monter/descendre donnent directement la
 * vitesse verticale.
 *
 * Les sous-classes fournissent leur vitesse, leur force de saut et leur
 * logique de tick (mettreAJour). La Map est partagée et jamais recréée ici.
 */
public abstract class Entite extends ElementDeJeu {
    protected int vitesseX;
    protected int vitesseY;
    protected int gravite = -6;
    protected int vitesseYMin = -24;
    protected final Map map;
    protected boolean monterDemande;
    protected boolean descendreDemande;
    protected boolean estSurEchelle;

    /**
     * @param map carte du niveau, utilisée pour toutes les collisions
     */
    public Entite(Map map, int positionX, int positionY) {
        super(positionX, positionY);
        this.map = map;
    }

    /** Entité placée à l'origine, positionnée plus tard. */
    public Entite(Map map) {
        this(map, 0, 0);
    }

    @Override
    protected int getTailleTuileDessin() {
        return map.getTailleTuile();
    }

    /** Applique la vitesse courante : axe X puis axe Y. */
    public void deplacer() {
        deplacerX();
        deplacerY();
    }

    /**
     * Avance horizontalement pixel par pixel jusqu'au premier obstacle.
     * Cas particulier : si le joueur incarne Etchebest et bute sur une caisse,
     * il tente de la pousser ; il n'est bloqué que si la caisse ne bouge pas.
     */
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

    /**
     * Avance verticalement pixel par pixel jusqu'au premier obstacle.
     * Le sommet d'une échelle se comporte comme un sol : on s'y pose, sauf si
     * le joueur demande à descendre.
     */
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

    /** Une caisse ne doit pas entrer en collision avec elle-même. */
    private boolean collisionPourEntite(int x, int y, int largeurHitbox, int hauteurHitbox) {
        if (this instanceof Caisse) {
            return map.collisionHitboxSansCaisses(x, y, largeurHitbox, hauteurHitbox,
                    (Caisse) this);
        }
        return map.collisionHitbox(x, y, largeurHitbox, hauteurHitbox);
    }

    /** @return true si un bloc solide est juste sous l'entité et qu'elle ne monte pas */
    protected boolean estAuSol() {
        int[] dimensionsHitbox = getHitbox();
        return map.aUnSolSous(positionX, positionY,
                dimensionsHitbox[0], dimensionsHitbox[1]) && vitesseY <= 0;
    }

    /** Mémorise les touches haut/bas, lues au prochain calcul de vitesse verticale. */
    public void definirCommandesEchelle(boolean monter, boolean descendre) {
        monterDemande = monter;
        descendreDemande = descendre;
    }

    /**
     * Calcule la vitesse verticale du tick. Ordre de priorité :
     * échelle (monter/descendre) &gt; saut &gt; immobile sur échelle &gt;
     * au sol &gt; chute avec gravité plafonnée.
     *
     * @param demandeSaut true si un saut est déclenché ce tick
     */
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

    /** @return vitesse de déplacement en pixels par tick */
    public abstract int getVitesse();

    /** @return vitesse verticale donnée au départ d'un saut (0 = ne saute pas) */
    public abstract int getForceSaut();

    /** Logique propre à l'entité pour un tick de jeu (intentions + déplacement). */
    public abstract void mettreAJour();

}
