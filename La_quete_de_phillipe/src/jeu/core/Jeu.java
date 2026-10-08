package jeu.core;
import java.util.*; import jeu.bdd.*; import jeu.entites.Avatar; import jeu.entites.monstres.Monstre;
/** Coeur: simulation locale et synchronisation periodique tolerante aux erreurs. */
public class Jeu { private final List<Avatar> avatars=new ArrayList<Avatar>(); private final List<Monstre> monstres=new ArrayList<Monstre>(); private Avatar avatarLocal; private boolean hote=true; private long frames; private final AvatarDAO avatarDAO=new AvatarDAO(); private final MonstreDAO monstreDAO=new MonstreDAO();
 public void ajouterAvatar(Avatar a){if(a!=null){avatars.add(a);if(a.isLocal())avatarLocal=a;}} public void ajouterMonstre(Monstre m){if(m!=null)monstres.add(m);} public void miseAJour(){frames++;if(avatarLocal!=null)avatarLocal.miseAJour();if(hote){for(Monstre m:monstres)m.miseAJour();}if(frames%5==0)synchroniser();}
 private void synchroniser(){try{if(hote)monstreDAO.envoyer(monstres);else{monstres.clear();monstres.addAll(monstreDAO.lireTous());}for(Avatar a:avatars)avatarDAO.envoyer(a);}catch(RuntimeException e){/* hors ligne */}}
 public List<Avatar> getAvatars(){return avatars;} public List<Monstre> getMonstres(){return monstres;} public Avatar getAvatarLocal(){return avatarLocal;} public boolean isHote(){return hote;} public void setHote(boolean hote){this.hote=hote;} public long getFrames(){return frames;}
}
