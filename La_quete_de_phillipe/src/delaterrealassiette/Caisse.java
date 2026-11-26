package delaterrealassiette;

/**
 * Caisse d'une tuile que seul Etchebest peut pousser.
 *
 * C'est une Entite : elle tombe et bloque les autres entités comme un mur.
 * Elle n'avance jamais d'elle-même (vitesse nulle) ; c'est la Map qui la
 * déplace quand elle est poussée (Map.pousserCaisse). Elle peut aussi
 * reposer sur le sommet d'une échelle.
 */
public class Caisse extends Entite {
    public Caisse(Map map, int positionX, int positionY) {
        super(map, positionX, positionY);
        hitbox = new int[]{map.getTailleTuile(), map.getTailleTuile()};
        cheminImage = "images/caisse.png";
    }

    @Override
    public int getVitesse() {
        return 0;
    }

    @Override
    public int getForceSaut() {
        return 0;
    }

    /** Tick : uniquement la chute sous l'effet de la gravité. */
    @Override
    public void mettreAJour() {
        vitesseX = 0;
        if (estAuSol()) {
            vitesseY = 0;
        } else {
            vitesseY = Math.max(vitesseY + gravite, vitesseYMin);
        }
        deplacer();
    }

    /**
     * Version propre aux caisses : la caisse est ignorée dans le test de
     * collision, sinon elle se considérerait toujours posée sur elle-même.
     */
    @Override
    protected boolean estAuSol() {
        return (map.collisionHitboxSansCaisses(positionX, positionY - 1,
                hitbox[0], hitbox[1], this)
                || map.estSurSommetEchelle(positionX, positionY, hitbox[0]))
                && vitesseY <= 0;
    }
}
