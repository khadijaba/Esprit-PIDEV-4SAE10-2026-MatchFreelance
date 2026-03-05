-- Fix "Data truncated for column 'category'" after adding new SkillCategory values.
-- Run this once on your MySQL database (e.g. use database "skill" or your skill DB name).

USE skill;

-- Allow all enum string values (e.g. CYBERSECURITY, MOBILE_DEVELOPMENT, etc.)
ALTER TABLE skill MODIFY COLUMN category VARCHAR(50) NOT NULL;
