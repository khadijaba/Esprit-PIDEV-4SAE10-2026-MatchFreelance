-- Idempotent : complète la table users si elle vient d’un ancien schéma (ex. sans birth_date).
-- Réexécuté par Flyway seulement si le checksum de ce fichier change.

DELIMITER $$

DROP PROCEDURE IF EXISTS tmp_align_users_for_jpa$$

CREATE PROCEDURE tmp_align_users_for_jpa()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users') THEN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'birth_date') THEN
      ALTER TABLE users ADD COLUMN birth_date DATE NOT NULL DEFAULT '2000-01-01';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'enabled') THEN
      ALTER TABLE users ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'face_descriptor') THEN
      ALTER TABLE users ADD COLUMN face_descriptor TEXT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'profile_picture_url') THEN
      ALTER TABLE users ADD COLUMN profile_picture_url VARCHAR(255) NULL;
    END IF;
  END IF;
END$$

DELIMITER ;

CALL tmp_align_users_for_jpa();

DROP PROCEDURE IF EXISTS tmp_align_users_for_jpa;
