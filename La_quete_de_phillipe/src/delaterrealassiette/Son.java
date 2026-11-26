package delaterrealassiette;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Effet sonore court (mort, victoire...), chargé une fois en mémoire et
 * rejoué autant de fois que nécessaire.
 *
 * Seul le format .wav est lu par Java (pas le .mp3). Un son absent ou
 * illisible n'empêche pas le jeu de tourner : un message est écrit dans la
 * console et jouer() ne fait alors plus rien.
 */
public class Son {

    /** null si le son n'a pas pu être chargé. */
    private Clip clip;

    /** @param cheminFichier chemin du .wav, relatif au dossier du projet (ex. "sons/mort.wav") */
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

    /** Joue le son depuis le début, en le relançant s'il était déjà en cours. */
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

    public boolean estCharge() {
        return clip != null;
    }

    /** Libère la ressource audio (à la fermeture de la fenêtre). */
    public void fermer() {
        if (clip != null) {
            clip.close();
            clip = null;
        }
    }
}
