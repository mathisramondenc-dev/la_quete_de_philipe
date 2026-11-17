package delaterrealassiette;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Travail JDBC hors EDT: une connexion persistante dédiée à ce thread. */
public final class SynchronisationBDD implements Runnable {
    private static final long INTERVALLE_JOUEURS_MS = 100L;
    private static final long INTERVALLE_MONDE_MS = 700L;
    private final Jeu jeu;
    private final InfosPartie infos;
    private final List<JoueurDistant> distants = new ArrayList<>();
    private volatile boolean actif;
    private Thread thread;
    public SynchronisationBDD(Jeu jeu, InfosPartie infos) { this.jeu=jeu; this.infos=infos; }
    public synchronized void demarrer() {
        if(actif || infos==null || infos.getSessionId()==null) return;
        actif=true; thread=new Thread(this,"SynchronisationBDD-"+infos.getPseudo()); thread.setDaemon(true); thread.start();
    }
    public synchronized void arreter() {
        actif=false;
        if(thread!=null) thread.interrupt();
        // DELETE de session hors EDT; le TTL de présence couvre aussi un arrêt forcé.
        Thread nettoyage=new Thread(() -> GestionBDD.quitter(infos), "NettoyageSessionBDD");
        nettoyage.setDaemon(true);
        nettoyage.start();
    }
    public List<JoueurDistant> getJoueursDistants() { return distants; }
    @Override public void run() {
        GestionBDD.EtatSynchronisation etat = new GestionBDD.EtatSynchronisation();
        long prochaineLectureMonde = 0L;
        while(actif) {
            Connection connexion = null;
            try {
                connexion = ConnexionBDD.getInstance().getConnection();
                if(connexion.isClosed()) throw new SQLException("Connexion fermée dès son ouverture.");
                while(actif) {
                    long maintenant = System.currentTimeMillis();
                    boolean lireMonde = maintenant >= prochaineLectureMonde;
                    GestionBDD.synchroniser(connexion, infos, jeu, distants, etat, lireMonde);
                    if(lireMonde) prochaineLectureMonde = maintenant + INTERVALLE_MONDE_MS;
                    Thread.sleep(INTERVALLE_JOUEURS_MS);
                }
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            } catch(SQLException e) {
                GestionBDD.signalerErreurSynchronisation(e);
                prochaineLectureMonde = 0L;
                try { Thread.sleep(1500L); }
                catch(InterruptedException stop) { Thread.currentThread().interrupt(); break; }
            } finally {
                if(connexion != null) try { connexion.close(); } catch(SQLException ignore) { }
            }
        }
    }
}
