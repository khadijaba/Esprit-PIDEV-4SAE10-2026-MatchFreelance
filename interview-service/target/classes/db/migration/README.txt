MySQL migration: create freelancing_interviews and move interview data from freelancing_candidature.

1. Run the migration (from project root or interview-service):
   mysql -u root -p < interview-service/src/main/resources/db/migration/mysql_create_and_migrate_interviews.sql

   Or open the .sql file in MySQL Workbench / DBeaver and execute.

2. If your old table in freelancing_candidature has different column names (e.g. startAt instead of start_at),
   edit the INSERT in the script and alias them: startAt AS start_at, etc.

3. Optional: After verifying data in freelancing_interviews, uncomment the DROP TABLE at the end of the script
   to remove the old interview table from freelancing_candidature (so no microservice uses it by mistake).

4. Start the interview-service with profile mysql: spring.profiles.active=mysql
   It will connect to freelancing_interviews and use tables: interviews, availability_slots, reviews, notifications.
