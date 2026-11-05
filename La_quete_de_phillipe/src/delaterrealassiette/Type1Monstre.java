/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package delaterrealassiette;

/**
 *
 * @author rlasri
 */
public class Type1Monstre extends Monstre {
    public Type1Monstre(Map map, int positionX, int positionY) {
        super(map, positionX, positionY,
              2,                  // vitesse (pixels par tick)
              10,                 // dégâts
              new int[]{32, 32},  // hitbox : largeur, hauteur
              "images/Type1Monstre.png");
    }
}
