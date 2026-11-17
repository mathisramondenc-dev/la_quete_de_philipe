// Son.java
package delaterrealassiette;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/* ________________________________________________________________
 * ACTION : Charge un son depuis un fichier .wav et le joue à la demande.
 * [INFO] Java lit les fichiers .wav, mais PAS les .mp3 : convertir le son
 *        en .wav avant (par exemple avec Audacity : Fichier > Exporter).
 * [INFO] Si le fichier est absent ou illisible, un message s'affiche dans
 *        la console et le jeu continue sans son (pas de plantage).
 * [DEPENDANCE] FenetreDeJeu joue le son de mort.
 * ________________________________________________________________ */
public class Son {

    private Clip clip; // null si le son n'a pas pu être chargé

    /* ________________________________________________________________
     * ACTION : Charge le son en mémoire.
     * [INFO] Le chemin est relatif au dossier du projet NetBeans,
     *        comme pour les images (ex : "sons/mort.wav").
     * ________________________________________________________________ */
    public Son(String cheminFichier) {
        try {
            AudioInputStream flux = AudioSystem.getAudioInputStream(new File(cheminFichier));
            clip = AudioSystem.getClip();
            clip.open(flux);
        } catch (Exception erreur) {
            System.err.println("Son non chargé (" + cheminFichier + ") : " + erreur.getMessage());
            clip = null;
        }
    }

    /* ________________________________________________________________
     * ACTION : Joue le son depuis le début.
     * [INFO] Si le son est déjà en train de jouer, il recommence.
     * ________________________________________________________________ */
    public void jouer() {
        if (clip == null) {
            return;
        }
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    /* ________________________________________________________________
     * ACTION : Indique si le son a bien été chargé.
     * ________________________________________________________________ */
    public boolean estCharge() {
        return clip != null;
    }

    /* ________________________________________________________________
     * ACTION : Libère le son (à appeler quand la fenêtre se ferme).
     * ________________________________________________________________ */
    public void fermer() {
        if (clip != null) {
            clip.close();
            clip = null;
        }
    }
}
