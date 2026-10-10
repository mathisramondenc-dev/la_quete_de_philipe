# la\_quete\_de\_philipe est un super jeu (bientôt)

arborescence actuelle a tenir a jour:
DeLaTerreALAssiette/
├── build.xml
├── manifest.mf
├── nbproject/
│   ├── project.properties
│   └── project.xml
└── src/delaterrealassiette/
    ├── Main.java            (main() vide + // TODO)
    ├── Jeu.java             (Carte, List<Entite>, GestionBDD)
    ├── GestionBDD.java      (Connection + demarrer/arreter/miseAJourImport/miseAJourExport)
    ├── Carte.java
    ├── Fenetre.java         (abstract)
    ├── FenetreMenu.java     extends Fenetre
    ├── FenetreDeJeu.java    extends Fenetre
    ├── Entite.java          (abstract, mettreAJour() abstract)
    ├── Joueur.java          extends Entite
    └── Monstre.java         (abstract) extends Entite



Lien du site pour tracer l'uml: https://www.plantuml.com/plantuml/uml/SyfFKj2rKt3CoKnELR1Io4ZDoSa700001
Il faut remplacer le code du debut par celui-ci (pensez à le tenir à jour après modification):
@startuml
skinparam classAttributeIconSize 0
skinparam linetype ortho
top to bottom direction

' ===== HAUT : AFFICHAGE / IHM =====
abstract class Fenetre
class FenetreMenu
class FenetreDeJeu

' ===== MILIEU : CŒUR =====
class Main
class Jeu
class GestionBDD {
  -connexion : Connection
  +demarrer()
  +arreter()
  +miseAJourImport()
  +miseAJourExport()
}

' ===== BAS : MÉCANIQUES INTERNES =====
class Carte

abstract class Entite {
  {static} -HITBOX_LARGEUR : int = 0
  {static} -HITBOX_HAUTEUR : int = 1
  #id : int
  #x : int
  #y : int
  #hitbox : int[2]
  #pv : int
  #pvMax : int
  #pas : int
  #porteeSaut : int
  -modifie : boolean
  --
  +mettreAJour() {abstract}
  +subirDegats(degats : int)
  +estVivant() : boolean
  --
  +getId() : int
  +getX() : int
  +getY() : int
  +getHitbox(index : int) : int
  +getHitboxLargeur() : int
  +getHitboxHauteur() : int
  +getPv() : int
  +getPvMax() : int
  +getPas() : int
  +getPorteeSaut() : int
  --
  +setPosition(x : int, y : int)
  +setPv(pv : int)
  --
  #marquerModifie()
  +estModifie() : boolean
  +effacerModifie()
}

class Joueur {
  -role : int
  -pseudo : String
  -peutUtiliserOutil : boolean
  --
  +mettreAJour()
  +changerRole(nouveau : int)
  -appliquerCapacites(role : int)
  +getRole() : int
  +getPseudo() : String
  +peutUtiliserOutil() : boolean
}

abstract class Monstre

' ===== RELATIONS =====
Fenetre <|-- FenetreMenu
Fenetre <|-- FenetreDeJeu

Main --> FenetreMenu : ouvre
FenetreMenu --> Jeu : lance la partie
Jeu --> FenetreDeJeu : ouvre

Jeu *-- Carte
Jeu *-- Entite
Jeu *-- GestionBDD

Entite <|-- Joueur
Entite <|-- Monstre

GestionBDD ..> Entite : lit / met à jour
GestionBDD ..> Carte : lit / met à jour
@enduml
