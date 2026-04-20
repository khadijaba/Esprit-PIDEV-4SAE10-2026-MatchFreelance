SELECT 'freelancing_users.users' AS tbl, COUNT(*) AS cnt FROM freelancing_users.users
UNION ALL
SELECT 'freelancing_projects.projects', COUNT(*) FROM freelancing_projects.projects
UNION ALL
SELECT 'freelancing_candidatures.candidatures', COUNT(*) FROM freelancing_candidatures.candidatures
UNION ALL
SELECT 'freelancing_contracts.contracts', COUNT(*) FROM freelancing_contracts.contracts;
