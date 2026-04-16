-- Supprimer la contrainte UNIQUE sur freelancer_id pour permettre plusieurs portfolios par freelancer (comme les skills).
-- Exécuter une seule fois sur la base skill (MySQL).
-- Si l'erreur indique un autre nom de contrainte, utiliser: SHOW INDEX FROM portfolio; puis DROP INDEX nom_affiché;

ALTER TABLE portfolio DROP INDEX UKee2ov6wwb4l917cgj4nbe7eol;
