package jeu.entites;

import java.awt.Color;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/** Avatar pilotable au clavier (fleches ou ZQSD). */
public class Avatar extends Entite implements KeyListener {
    protected int id; protected boolean local;
    protected boolean haut, bas, gauche, droite;
    public Avatar(int id,int x,int y,boolean local,Image sprite){super(x,y,3,sprite!=null?sprite:spriteParDefaut(Color.RED));this.id=id;this.local=local;}
    public Avatar(int id,int x,int y,boolean local){this(id,x,y,local,null);}
    public void miseAJour(){ if(!local)return; if(haut)y-=vitesse; if(bas)y+=vitesse; if(gauche)x-=vitesse; if(droite)x+=vitesse; }
    public void keyTyped(KeyEvent e){}
    public void keyPressed(KeyEvent e){switch(e.getKeyCode()){case KeyEvent.VK_UP:case KeyEvent.VK_Z:haut=true;break;case KeyEvent.VK_DOWN:case KeyEvent.VK_S:bas=true;break;case KeyEvent.VK_LEFT:case KeyEvent.VK_Q:gauche=true;break;case KeyEvent.VK_RIGHT:case KeyEvent.VK_D:droite=true;break;default:;}}
    public void keyReleased(KeyEvent e){switch(e.getKeyCode()){case KeyEvent.VK_UP:case KeyEvent.VK_Z:haut=false;break;case KeyEvent.VK_DOWN:case KeyEvent.VK_S:bas=false;break;case KeyEvent.VK_LEFT:case KeyEvent.VK_Q:gauche=false;break;case KeyEvent.VK_RIGHT:case KeyEvent.VK_D:droite=false;break;default:;}}
    public int getId(){return id;} public boolean isLocal(){return local;} public void setLocal(boolean local){this.local=local;}
}
