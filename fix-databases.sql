-- Fix orphaned tablespace files by dropping and recreating databases
-- Run this in MySQL (phpMyAdmin SQL tab or MySQL command line)

DROP DATABASE IF EXISTS freelancing_interviews;
DROP DATABASE IF EXISTS freelancing_projects;
DROP DATABASE IF EXISTS freelancing_users;
DROP DATABASE IF EXISTS freelancing_candidatures;
DROP DATABASE IF EXISTS freelancing_contracts;

CREATE DATABASE freelancing_interviews CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE freelancing_projects CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE freelancing_users CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE freelancing_candidatures CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE freelancing_contracts CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
