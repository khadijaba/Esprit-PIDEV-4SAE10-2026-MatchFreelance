-- Reset MatchFreelance so seed data loads on next service startup.
-- Creates databases if missing; drops tables so apps recreate them and run DataLoaders.

SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS freelancing_contracts;
CREATE DATABASE IF NOT EXISTS freelancing_candidatures;
CREATE DATABASE IF NOT EXISTS freelancing_projects;
CREATE DATABASE IF NOT EXISTS freelancing_users;

USE freelancing_contracts;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS contracts;

USE freelancing_candidatures;
DROP TABLE IF EXISTS interviews;
DROP TABLE IF EXISTS candidatures;

USE freelancing_projects;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS projects;

USE freelancing_users;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;
