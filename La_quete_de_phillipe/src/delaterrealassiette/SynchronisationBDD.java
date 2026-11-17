package delaterrealassiette;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Travail JDBC hors EDT: publication de la position et lecture des autres joueurs. */
public final class SynchronisationBDD implements Runnable {
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
        actif=false; if(thread!=null) thread.interrupt(); GestionBDD.quitter(infos);
    }
    public List<JoueurDistant> getJoueursDistants() { return distants; }
    @Override public void run() {
        // === MULTIJOUEUR === JDBC bloquant isolé du thread Swing (EDT).
        while(actif) {
            try(Connection c=ConnexionBDD.getInstance().getConnection()) {
                while(actif) {
                    GestionBDD.synchroniser(c,infos,jeu,distants);
                    Thread.sleep(250);
                }
            } catch(InterruptedException e) { Thread.currentThread().interrupt(); break; }
              catch(SQLException e) { System.err.println("Synchronisation multijoueur: "+e.getMessage()); try { Thread.sleep(1500); } catch(InterruptedException stop){Thread.currentThread().interrupt();break;} }
        }
    }
}
