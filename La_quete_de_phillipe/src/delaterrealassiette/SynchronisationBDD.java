package delaterrealassiette;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread de synchronisation multijoueur, un par fenêtre de jeu connectée.
 *
 * Les requêtes SQL peuvent prendre du temps : les faire dans la boucle de
 * jeu (thread Swing) ferait saccader l'affichage. Ce thread tourne donc à
 * part et, toutes les 250 ms, appelle GestionBDD.synchroniser() qui publie
 * la position du joueur et ses actions, puis lit les autres joueurs et
 * l'état partagé.
 *
 * En cas d'erreur SQL (réseau coupé...), la connexion est rouverte après
 * 1,5 s. arreter() stoppe le thread et retire le joueur de la partie.
 */
public final class SynchronisationBDD implements Runnable {
    private final Jeu jeu;
    private final InfosPartie infos;
    /** Autres joueurs lus en base ; accès protégé par synchronized(distants). */
    private final List<JoueurDistant> distants = new ArrayList<>();
    private volatile boolean actif;
    private Thread thread;
    public SynchronisationBDD(Jeu jeu, InfosPartie infos) { this.jeu=jeu; this.infos=infos; }
    /** Lance le thread (sans effet s'il tourne déjà ou s'il n'y a pas de session). */
    public synchronized void demarrer() {
        if(actif || infos==null || infos.getSessionId()==null) return;
        actif=true; thread=new Thread(this,"SynchronisationBDD-"+infos.getPseudo()); thread.setDaemon(true); thread.start();
    }
    /** Arrête le thread et supprime la session du joueur dans la base. */
    public synchronized void arreter() {
        actif=false; if(thread!=null) thread.interrupt(); GestionBDD.quitter(infos);
    }
    public List<JoueurDistant> getJoueursDistants() { return distants; }
    /** Boucle du thread : une connexion ouverte, puis une synchronisation toutes les 250 ms. */
    @Override public void run() {
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
