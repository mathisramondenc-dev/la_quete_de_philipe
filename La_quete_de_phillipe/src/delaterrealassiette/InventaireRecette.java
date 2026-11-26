package delaterrealassiette;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Recette du niveau : quels ingrédients ramasser, combien, et où en est le
 * joueur.
 *
 * La recette n'est pas écrite à part : elle est déduite des ingrédients
 * posés dans le niveau (deux tomates sur la carte = deux tomates requises).
 * Le niveau est réussi quand toutes les quantités requises sont atteintes.
 *
 * La classe dessine aussi le panneau récapitulatif en haut à droite de
 * l'écran (icône grisée tant que l'ingrédient n'a pas été trouvé).
 */
public class InventaireRecette {
    private final EnumMap<TypeIngredient, Integer> quantitesRequises = new EnumMap<>(TypeIngredient.class);
    private final EnumMap<TypeIngredient, Integer> quantitesRamassees = new EnumMap<>(TypeIngredient.class);

    /** @param ingredients ingrédients présents dans le niveau : ils définissent la recette */
    public InventaireRecette(List<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            TypeIngredient typeIngredient = ingredient.getTypeIngredient();
            int quantiteRequise = quantitesRequises.getOrDefault(typeIngredient, 0);
            quantitesRequises.put(typeIngredient, quantiteRequise + 1);
        }
        for (TypeIngredient typeIngredient : quantitesRequises.keySet()) {
            quantitesRamassees.put(typeIngredient, 0);
        }
    }

    /** Compte un ingrédient ramassé, sans dépasser la quantité requise. */
    public void ajouter(TypeIngredient typeIngredient) {
        int quantiteRamassee = quantitesRamassees.getOrDefault(typeIngredient, 0);
        int quantiteRequise = quantitesRequises.getOrDefault(typeIngredient, 0);
        if (quantiteRamassee < quantiteRequise) {
            quantitesRamassees.put(typeIngredient, quantiteRamassee + 1);
        }
    }

    /** @return true si tous les ingrédients requis ont été ramassés */
    public boolean estComplete() {
        for (Map.Entry<TypeIngredient, Integer> entree : quantitesRequises.entrySet()) {
            if (quantitesRamassees.getOrDefault(entree.getKey(), 0) < entree.getValue()) {
                return false;
            }
        }
        return true;
    }

    public int getQuantiteRequise(TypeIngredient typeIngredient) {
        return quantitesRequises.getOrDefault(typeIngredient, 0);
    }

    public int getQuantiteRamassee(TypeIngredient typeIngredient) {
        return quantitesRamassees.getOrDefault(typeIngredient, 0);
    }

    /** @return copie, modifiable sans effet sur l'inventaire */
    public Map<TypeIngredient, Integer> getQuantitesRequises() {
        return new EnumMap<>(quantitesRequises);
    }

    /** @return copie, modifiable sans effet sur l'inventaire */
    public Map<TypeIngredient, Integer> getQuantitesRamassees() {
        return new EnumMap<>(quantitesRamassees);
    }

    /**
     * Dessine le panneau de recette en haut à droite : une ligne par type
     * (icône + "Nom ramassé/requis"), hauteur adaptée au nombre de types.
     *
     * @param largeurEcran largeur de la zone de dessin, pour l'aligner à droite
     */
    public void dessiner(Graphics2D contexte, int largeurEcran) {
        int hauteurPanneau = 18 + quantitesRequises.size() * 42;
        int largeurPanneau = 240;
        int positionX = largeurEcran - largeurPanneau - 12;
        int positionY = 12;
        java.awt.Composite compositePrecedent = contexte.getComposite();
        contexte.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        contexte.setColor(Color.BLACK);
        contexte.fillRoundRect(positionX, positionY, largeurPanneau, hauteurPanneau, 14, 14);
        contexte.setComposite(compositePrecedent);
        contexte.setFont(new Font("SansSerif", Font.BOLD, 14));
        int numeroLigne = 0;
        for (Map.Entry<TypeIngredient, Integer> entree : quantitesRequises.entrySet()) {
            TypeIngredient typeIngredient = entree.getKey();
            int quantiteRequise = entree.getValue();
            int positionLigneY = positionY + 32 + numeroLigne * 42;
            BufferedImage image = ElementDeJeu.chargerImageDepuisCache(typeIngredient.getCheminSprite());
            if (image != null) {
                // Pas encore trouvé : icône sur fond gris et semi-transparente
                if (getQuantiteRamassee(typeIngredient) == 0) {
                    contexte.setColor(Color.GRAY);
                    contexte.fillRect(positionX + 12, positionLigneY - 20, 30, 30);
                    contexte.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                    contexte.drawImage(image, positionX + 12, positionLigneY - 20, 30, 30, null);
                    contexte.setComposite(compositePrecedent);
                } else {
                    contexte.drawImage(image, positionX + 12, positionLigneY - 20, 30, 30, null);
                }
            }
            contexte.setColor(Color.WHITE);
            contexte.drawString(typeIngredient.getNom() + "  " + getQuantiteRamassee(typeIngredient)
                    + "/" + quantiteRequise, positionX + 52, positionLigneY);
            numeroLigne++;
        }
    }
}
