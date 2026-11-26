-- Migration manuelle MariaDB : les données de ces tables sont des états de partie temporaires.
-- Les tables anciennes peuvent ne pas avoir niveau_id ; DROP + CREATE garantit aussi
-- la clé primaire attendue (code_partie, niveau_id, identifiant objet).
DROP TABLE IF EXISTS levier_multi;
DROP TABLE IF EXISTS ingredient_multi;

CREATE TABLE levier_multi (
    code_partie VARCHAR(40) NOT NULL,
    niveau_id INT NOT NULL,
    levier_id INT NOT NULL,
    actif BOOLEAN NOT NULL,
    PRIMARY KEY (code_partie, niveau_id, levier_id),
    CONSTRAINT fk_levier_multi_partie FOREIGN KEY (code_partie)
        REFERENCES partie_multi(code_partie) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ingredient_multi (
    code_partie VARCHAR(40) NOT NULL,
    niveau_id INT NOT NULL,
    ingredient_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (code_partie, niveau_id, ingredient_id),
    CONSTRAINT fk_ingredient_multi_partie FOREIGN KEY (code_partie)
        REFERENCES partie_multi(code_partie) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Complète aussi les anciennes tables de partie/joueur sans toucher aux données existantes.
ALTER TABLE partie_multi ADD COLUMN IF NOT EXISTS niveau_courant INT NOT NULL DEFAULT 1;
ALTER TABLE joueur_multi ADD COLUMN IF NOT EXISTS avatar_actuel VARCHAR(40) NOT NULL DEFAULT 'Etchebest';
