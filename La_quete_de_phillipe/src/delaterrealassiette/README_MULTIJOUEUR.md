# Multijoueur MariaDB — La Quête de Philippe

## Fonctionnalités raccordées

Les tables de monde sont désormais exploitées par `GestionBDD.synchroniserMonde`, appelé depuis le thread daemon `SynchronisationBDD` (toutes les 250 ms). Les requêtes emploient `PreparedStatement` avec try-with-resources. Les mutations des objets Swing passent par `SwingUtilities.invokeLater` vers l’EDT.

- **Ingrédients** : au premier cycle BDD de l’hébergeur, `INSERT IGNORE` photographie les ingrédients du niveau avec index stable, type et coordonnées. Une collecte locale est publiée avec `UPDATE ... SET ramasse_par=? ... AND ramasse_par IS NULL`. Tous les clients lisent les collectes et désactivent ces ingrédients sur l’EDT. `InventaireRecette` demeure strictement individuel : chaque joueur ne progresse dans sa recette que pour les ingrédients qu’il a ramassés lui-même localement.
- **Monstres** : indexés dans l’ordre de la carte. Seul l’hébergeur appelle l’IA et publie position, PV, vivant et orientation. Les clients sautent l’IA et appliquent les instantanés lus en BDD sur l’EDT. `Jeu.demanderDegatsMonstre(index, degats)` fournit le hook d’attaque client avec un `UPDATE monstre_multi SET pv=pv-?`; le projet courant ne possède toutefois pas de mécanisme d’attaque contre les monstres qui puisse appeler cette API.
- **Leviers** : indexés par ordre dans la carte. Leur bascule enfile `actif` pour publication `UPDATE levier_multi SET actif=?`; tous relisent l’état puis l’appliquent au levier sur l’EDT. `Map.mettreAJourMecanismes` applique ensuite les états existants aux blocs-portes et à leurs collisions.
- **Joueurs distants** : `JoueurDistant` dessine les sprites locaux propres à l’avatar et respecte l’orientation miroir. `FenetreDeJeu` appelle le rendu des distants.

L’initialisation du monde a lieu dès le premier cycle de synchronisation du niveau par l’hébergeur (les tables ne sont pas insérées à la création du salon puisque la carte n’est pas encore chargée). `INSERT IGNORE` conserve la photographie initiale après la reconnexion. Les clés de monde sont `code_partie,niveau,index`.

## Schéma SQL multijoueur

Créé par `CreationBDD` (`ENGINE=InnoDB`, clés étrangères vers `partie_multi` avec cascade) :

- `partie_multi(code_partie PK, mot_de_passe, cree_le)` : salons.
- `joueur_multi(session_id PK, code_partie, pseudo, position_x, position_y, avatar_actuel, direction_gauche, en_mouvement, dernier_ping, UNIQUE(code_partie,pseudo))` : présence, position et rendu.
- `ingredient_multi(code_partie, niveau, ingredient_id, x, y, type, ramasse_par, PK(code_partie,niveau,ingredient_id))` : état de collecte partagé.
- `monstre_multi(code_partie, niveau, monstre_id, type, x, y, pv, vivant, direction_gauche, degats_demandes, PK(code_partie,niveau,monstre_id))` : état autoritaire des monstres. Le champ `degats_demandes` n’est pas utilisé, les demandes sont des mises à jour directes atomiques de `pv`.
- `levier_multi(code_partie, niveau, levier_id, id_mecanisme, actif, PK(code_partie,niveau,levier_id))` : état de chaque levier.

Les tables historiques (`avatar`, `carte`, `joueur`, `monstre`, `tuile`) restent inchangées. Les salons et leurs données ne sont pas purgés automatiquement; seule la présence des joueurs obsolètes est filtrée.

## Changements fichier par fichier

- `CreationBDD.java` : DDL des tables de session et de monde, puis migration MariaDB additive/idempotente des colonnes multijoueur susceptibles de manquer (notamment orientation et animation distante). Il faut relancer `CreationBDD` après mise à jour.
- `InfosPartie.java` : prédicats multijoueur et hébergeur.
- `Jeu.java` : configuration, index stables, file de collecte et de bascule, désactivation de l’IA client, application des instantanés, hook public de dégâts.
- `Map.java`, `Ingredient.java`, `Levier.java` : retrait/états et index stable.
- `Monstre.java` : PV/vivant, index, autorité IA et application de snapshots.
- `GestionBDD.java` : initialisation/lecture/écriture JDBC des trois tables de monde, publication directe des dégâts, snapshots.
- `SynchronisationBDD.java` : boucle JDBC hors EDT, réutilisant les méthodes BDD.
- `FenetreDeJeu.java`, `Joueur.java`, `JoueurDistant.java` : configuration de session, état de joueur distant et rendu par vrais sprites.
- `README_MULTIJOUEUR.md` : architecture, schéma, lancement et limites.

## Lancement

1. Compiler toutes les sources du package en incluant `mariadb-java-client` (version 3.4.1 testée pour compilation).
2. Définir `laquete.db.host`, `.port`, `.name`, `.user`, `.password` ou les variables `LAQUETE_DB_HOST`, `LAQUETE_DB_PORT`, `LAQUETE_DB_NAME`, `LAQUETE_DB_USER`, `LAQUETE_DB_PASSWORD`.
3. Lancer `delaterrealassiette.CreationBDD` avec les droits nécessaires, et **la relancer après chaque mise à jour du projet** : après les `CREATE TABLE IF NOT EXISTS`, le programme exécute à chaque lancement les `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` MariaDB du schéma multijoueur. Cette migration additive préserve les données et corrige notamment les anciennes tables `joueur_multi` sans `direction_gauche` ou `en_mouvement`. Elle ne remplace pas la création/réparation des colonnes de clés primaires ou étrangères.
4. Lancer `delaterrealassiette.Main`; héberger une partie puis ouvrir une seconde instance et la rejoindre avec les mêmes identifiants.
5. Garder disponibles dans chaque instance le même niveau et les assets. Les sprites distants utilisent `images/sprites/{etchebest,lignac,tarayre}.png`.

## Références pédagogiques

- `polyInfo2.pdf`, tutoriel JDBC, p. 9–13 : connexion, `PreparedStatement`, paramètres, exécution et lecture de `ResultSet`.
- `polyInfo2.pdf`, tutoriel JDBC, p. 25 : centralisation de connexions/Singelton; le projet conserve une connexion exclusive par thread.
- `polyInfo2.pdf`, réalisation du jeu, p. 17 : boucle `javax.swing.Timer` conservée à 40 ms.
- `polyInfo2.pdf`, section threads/EDT, p. 31, et `polyInfo1.pdf`, section threads/EDT, p. 63 : travail JDBC bloquant hors thread graphique et modification UI sur l’EDT avec `SwingUtilities.invokeLater`.
- `PreparedStatement` et try-with-resources sont appliqués à toutes les requêtes de cette synchronisation pour les paramètres et la fermeture déterministe.

## Limites restantes

- Aucun test d’intégration à MariaDB n’a été réalisé; compilation seulement. Le schéma doit être créé ou mis à niveau en relançant `CreationBDD` avant lancement. La migration additive répare les colonnes, mais ne reconstruit pas les clés/index manquants d’une table manuellement altérée.
- La collecte est mise en inventaire locale avant confirmation de la mise à jour BDD; si deux joueurs se chevauchent exactement au même moment, les deux inventaires locaux peuvent être incrémentés alors qu’un seul `ramasse_par` gagne. Les inventaires restent personnels par conception.
- Le jeu ne définit pas d’attaque contre les monstres : `demanderDegatsMonstre` est un hook prêt à brancher, mais n’est pas appelé par une action existante. Les dégâts de contact actuels sont infligés par chaque client à son joueur local.
- Les coordonnées des monstres/joueurs sont relayées sans interpolation ni réconciliation physique. La progression de niveau, les morts/respawns et les recettes/inventaires ne sont pas partagés.
- Mot de passe de salon stocké en clair, secrets de connexion à configurer pour l’environnement; prévoir hachage et contrôle côté serveur pour production.
