CREATE TABLE IF NOT EXISTS formation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  titre VARCHAR(255) NOT NULL,
  type_formation VARCHAR(255) NULL,
  description VARCHAR(2000) NULL,
  duree_heures INT NOT NULL,
  date_debut DATE NOT NULL,
  date_fin DATE NOT NULL,
  capacite_max INT NULL,
  statut VARCHAR(255) NOT NULL,
  niveau VARCHAR(20) NULL,
  examen_requis_id BIGINT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS module_formation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  titre VARCHAR(255) NOT NULL,
  description VARCHAR(2000) NULL,
  duree_minutes INT NOT NULL,
  ordre INT NOT NULL,
  formation_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_module_formation_formation_id (formation_id),
  CONSTRAINT fk_module_formation_formation
    FOREIGN KEY (formation_id) REFERENCES formation(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inscription (
  id BIGINT NOT NULL AUTO_INCREMENT,
  freelancer_id BIGINT NOT NULL,
  formation_id BIGINT NOT NULL,
  statut VARCHAR(255) NOT NULL,
  date_inscription DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inscription_formation_freelancer (formation_id, freelancer_id),
  KEY idx_inscription_formation_id (formation_id),
  CONSTRAINT fk_inscription_formation
    FOREIGN KEY (formation_id) REFERENCES formation(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
