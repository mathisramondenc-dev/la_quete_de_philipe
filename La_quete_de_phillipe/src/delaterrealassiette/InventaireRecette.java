package delaterrealassiette;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;

/* ________________________________________________________________
 * ACTION : Suit les quantités requises et ramassées pour la recette du niveau.
 * [DEPENDANCE] Jeu détient l'inventaire, alimenté par les ingrédients du niveau.
 * ________________________________________________________________ */
public class InventaireRecette {
    private final EnumMap<TypeIngredient, Integer> quantitesRequises = new EnumMap<>(TypeIngredient.class);
    private final EnumMap<TypeIngredient, Integer> quantitesRamassees = new EnumMap<>(TypeIngredient.class);

    public InventaireRecette(List<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            TypeIngredient type = ingredient.getTypeIngredient();
            quantitesRequises.put(type, quantitesRequises.getOrDefault(type, 0) + 1);
        }
        for (TypeIngredient type : quantitesRequises.keySet()) {
            quantitesRamassees.put(type, 0);
        }
    }

    /* ________________________________________________________________
     * ACTION : Ajoute une unité ramassée, limitée à la quantité de recette.
     * ________________________________________________________________ */
    public void ajouter(TypeIngredient type) {
        int ramassee = quantitesRamassees.getOrDefault(type, 0);
        int requise = quantitesRequises.getOrDefault(type, 0);
        if (ramassee < requise) {
            quantitesRamassees.put(type, ramassee + 1);
        }
    }

    public boolean estComplete() {
        for (java.util.Map.Entry<TypeIngredient, Integer> entree : quantitesRequises.entrySet()) {
            if (quantitesRamassees.getOrDefault(entree.getKey(), 0) < entree.getValue()) {
                return false;
            }
        }
        return true;
    }

    /* ________________________________________________________________
     * ACTION : Retourne le nombre requis pour un type d’ingrédient.
     * ________________________________________________________________ */
    public int getQuantiteRequise(TypeIngredient type) { return quantitesRequises.getOrDefault(type, 0); }

    /* ________________________________________________________________
     * ACTION : Retourne le nombre ramassé pour un type d’ingrédient.
     * ________________________________________________________________ */
    public int getQuantiteRamassee(TypeIngredient type) { return quantitesRamassees.getOrDefault(type, 0); }

    /* ________________________________________________________________
     * ACTION : Retourne une copie des quantités requises.
     * ________________________________________________________________ */
    public java.util.Map<TypeIngredient, Integer> getQuantitesRequises() { return new EnumMap<>(quantitesRequises); }

    /* ________________________________________________________________
     * ACTION : Retourne une copie des quantités ramassées.
     * ________________________________________________________________ */
    public java.util.Map<TypeIngredient, Integer> getQuantitesRamassees() { return new EnumMap<>(quantitesRamassees); }

    /* ________________________________________________________________
     * ACTION : Dessine en haut à droite le récapitulatif semi-transparent.
     * ________________________________________________________________ */
    public void dessiner(Graphics2D contexte, int largeurEcran) {
        int hauteurPanneau = 18 + quantitesRequises.size() * 42;
        int largeurPanneau = 240;
        int x = largeurEcran - largeurPanneau - 12;
        int y = 12;
        java.awt.Composite compositePrecedent = contexte.getComposite();
        contexte.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        contexte.setColor(Color.BLACK);
        contexte.fillRoundRect(x, y, largeurPanneau, hauteurPanneau, 14, 14);
        contexte.setComposite(compositePrecedent);
        contexte.setFont(new Font("SansSerif", Font.BOLD, 14));
        int ligne = 0;
        for (TypeIngredient type : TypeIngredient.values()) {
            int requise = getQuantiteRequise(type);
            if (requise == 0) continue;
            int positionY = y + 32 + ligne * 42;
            BufferedImage image = ElementDeJeu.chargerImageDepuisCache(type.getCheminSprite());
            if (image != null) {
                if (getQuantiteRamassee(type) == 0) {
                    contexte.setColor(Color.GRAY);
                    contexte.fillRect(x + 12, positionY - 20, 30, 30);
                    contexte.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                    contexte.drawImage(image, x + 12, positionY - 20, 30, 30, null);
                    contexte.setComposite(compositePrecedent);
                } else {
                    contexte.drawImage(image, x + 12, positionY - 20, 30, 30, null);
                }
            }
            contexte.setColor(Color.WHITE);
            contexte.drawString(type.getNom() + "  " + getQuantiteRamassee(type) + "/" + requise, x + 52, positionY);
            ligne++;
        }
    }
}
