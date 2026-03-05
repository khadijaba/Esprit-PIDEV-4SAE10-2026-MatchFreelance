-- =============================================================================
-- Migration: Create freelancing_interviews database and move interview data
-- from freelancing_candidature.interview into the new database.
-- Run this script once with MySQL (e.g. mysql -u root -p < this_file.sql).
-- =============================================================================

-- 1. Create the new database (no effect if it already exists)
CREATE DATABASE IF NOT EXISTS freelancing_interviews
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE freelancing_interviews;

-- 2. Create interviews table (full schema: mode, meeting type, location, visio, etc.)
CREATE TABLE IF NOT EXISTS interviews (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  candidature_id BIGINT NULL,
  project_id BIGINT NULL,
  freelancer_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  slot_id BIGINT NULL,
  start_at TIMESTAMP(6) NOT NULL,
  end_at TIMESTAMP(6) NOT NULL,
  mode VARCHAR(50) NOT NULL DEFAULT 'ONLINE',
  meeting_url VARCHAR(2000) NULL,
  address_line VARCHAR(500) NULL,
  city VARCHAR(200) NULL,
  lat DOUBLE NULL,
  lng DOUBLE NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'PROPOSED',
  notes VARCHAR(2000) NULL,
  visio_room_id VARCHAR(100) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_interview_freelancer_start (freelancer_id, start_at),
  INDEX idx_interview_owner_start (owner_id, start_at),
  INDEX idx_interview_status (status)
) ENGINE=InnoDB;

-- 3. Create availability_slots table
CREATE TABLE IF NOT EXISTS availability_slots (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  freelancer_id BIGINT NOT NULL,
  start_at TIMESTAMP(6) NOT NULL,
  end_at TIMESTAMP(6) NOT NULL,
  booked BOOLEAN NOT NULL DEFAULT FALSE,
  booked_interview_id BIGINT NULL,
  INDEX idx_slot_freelancer_start (freelancer_id, start_at),
  INDEX idx_slot_booked (booked)
) ENGINE=InnoDB;

-- 4. Create reviews table
CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  interview_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  reviewee_id BIGINT NOT NULL,
  score INT NOT NULL,
  comment VARCHAR(2000) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_review_interview (interview_id),
  INDEX idx_review_reviewee (reviewee_id),
  UNIQUE KEY uk_review_interview_reviewer (interview_id, reviewer_id)
) ENGINE=InnoDB;

-- 5. Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  interview_id BIGINT NOT NULL,
  type VARCHAR(50) NOT NULL,
  message VARCHAR(500) NULL,
  read_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_notification_user_created (user_id, created_at),
  INDEX idx_notification_interview_type (interview_id, type)
) ENGINE=InnoDB;

-- 6. Copy data from freelancing_candidature.interview into freelancing_interviews.interviews
--    Uses minimal column set from source so it works even if the old table has no mode/meeting_url/etc.
--    New columns in interviews: mode (default ONLINE), meeting_url, address_line, city, lat, lng, visio_room_id (all NULL if missing in source).
--    If your old table uses different column names (e.g. startAt), alias them: startAt AS start_at.
--    If the old table is named "interviews" (plural), change FROM to freelancing_candidature.interviews.
INSERT INTO freelancing_interviews.interviews (
  id, candidature_id, project_id, freelancer_id, owner_id, slot_id,
  start_at, end_at, mode, meeting_url, address_line, city, lat, lng,
  status, notes, visio_room_id, created_at
)
SELECT
  i.id,
  i.candidature_id,
  i.project_id,
  i.freelancer_id,
  i.owner_id,
  NULL,
  i.start_at,
  i.end_at,
  'ONLINE',
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  IFNULL(i.status, 'PROPOSED'),
  NULL,
  NULL,
  IFNULL(i.created_at, CURRENT_TIMESTAMP(6))
FROM freelancing_candidature.interview i
WHERE NOT EXISTS (SELECT 1 FROM freelancing_interviews.interviews t WHERE t.id = i.id);

-- If your old table also has mode, meeting_url, address_line, city, lat, lng, notes, visio_room_id,
-- you can run the following instead (and skip the INSERT above) to preserve that data:
--
-- INSERT INTO freelancing_interviews.interviews (
--   id, candidature_id, project_id, freelancer_id, owner_id, slot_id,
--   start_at, end_at, mode, meeting_url, address_line, city, lat, lng,
--   status, notes, visio_room_id, created_at
-- )
-- SELECT i.id, i.candidature_id, i.project_id, i.freelancer_id, i.owner_id, NULL,
--   i.start_at, i.end_at, COALESCE(i.mode,'ONLINE'), i.meeting_url, i.address_line, i.city, i.lat, i.lng,
--   COALESCE(i.status,'PROPOSED'), i.notes, i.visio_room_id, COALESCE(i.created_at, CURRENT_TIMESTAMP(6))
-- FROM freelancing_candidature.interview i
-- WHERE NOT EXISTS (SELECT 1 FROM freelancing_interviews.interviews t WHERE t.id = i.id);

-- 7. (Optional) After verifying data in freelancing_interviews, drop the old interview table
--    from freelancing_candidature so it is no longer used. Uncomment only when you are sure.
-- USE freelancing_candidature;
-- DROP TABLE IF EXISTS interview;
