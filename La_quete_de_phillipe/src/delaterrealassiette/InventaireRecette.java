package delaterrealassiette;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/* ________________________________________________________________
 * ACTION : Suit les quantités requises et ramassées pour la recette du niveau.
 * [DEPENDANCE] Jeu fournit les ingrédients présents dans le niveau.
 * ________________________________________________________________ */
public class InventaireRecette {
    private final EnumMap<TypeIngredient, Integer> quantitesRequises = new EnumMap<>(TypeIngredient.class);
    private final EnumMap<TypeIngredient, Integer> quantitesRamassees = new EnumMap<>(TypeIngredient.class);

    /* ________________________________________________________________
     * ACTION : Initialise l’inventaire à partir des ingrédients du niveau.
     * [INFO] Seuls les types présents sont conservés, avec une quantité positive.
     * ________________________________________________________________ */
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

    /* ________________________________________________________________
     * ACTION : Ajoute une unité ramassée, sans dépasser la quantité requise.
     * ________________________________________________________________ */
    public void ajouter(TypeIngredient typeIngredient) {
        int quantiteRamassee = quantitesRamassees.getOrDefault(typeIngredient, 0);
        int quantiteRequise = quantitesRequises.getOrDefault(typeIngredient, 0);
        if (quantiteRamassee < quantiteRequise) {
            quantitesRamassees.put(typeIngredient, quantiteRamassee + 1);
        }
    }

    /* ________________________________________________________________
     * ACTION : Indique si tous les ingrédients requis ont été ramassés.
     * ________________________________________________________________ */
    public boolean estComplete() {
        for (Map.Entry<TypeIngredient, Integer> entree : quantitesRequises.entrySet()) {
            if (quantitesRamassees.getOrDefault(entree.getKey(), 0) < entree.getValue()) {
                return false;
            }
        }
        return true;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le nombre requis pour un type d’ingrédient.
     * ________________________________________________________________ */
    public int getQuantiteRequise(TypeIngredient typeIngredient) {
        return quantitesRequises.getOrDefault(typeIngredient, 0);
    }

    /* ________________________________________________________________
     * ACTION : Retourne le nombre ramassé pour un type d’ingrédient.
     * ________________________________________________________________ */
    public int getQuantiteRamassee(TypeIngredient typeIngredient) {
        return quantitesRamassees.getOrDefault(typeIngredient, 0);
    }

    /* ________________________________________________________________
     * ACTION : Retourne une copie des quantités requises.
     * ________________________________________________________________ */
    public Map<TypeIngredient, Integer> getQuantitesRequises() {
        return new EnumMap<>(quantitesRequises);
    }

    /* ________________________________________________________________
     * ACTION : Retourne une copie des quantités ramassées.
     * ________________________________________________________________ */
    public Map<TypeIngredient, Integer> getQuantitesRamassees() {
        return new EnumMap<>(quantitesRamassees);
    }

    /* ________________________________________________________________
     * ACTION : Dessine le récapitulatif des types d’ingrédients requis.
     * [INFO] La hauteur du panneau dépend du nombre de types requis.
     * ________________________________________________________________ */
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
