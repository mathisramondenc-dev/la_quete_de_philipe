-- Le niveau initial des parties existantes est conservé à 1 ; la valeur devient ensuite partagée.
ALTER TABLE partie_multi ADD COLUMN IF NOT EXISTS niveau_courant INT NOT NULL DEFAULT 1;
