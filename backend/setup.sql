-- =====================================================
-- UMAT AI Quiz System — Database Setup Script
-- Run this before starting the backend for the first time
-- =====================================================

-- Create database
CREATE DATABASE IF NOT EXISTS umat_quiz
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE umat_quiz;

-- Create application user (optional — more secure than root)
-- CREATE USER 'umat_app'@'localhost' IDENTIFIED BY 'SecurePassword2026!';
-- GRANT ALL PRIVILEGES ON umat_quiz.* TO 'umat_app'@'localhost';
-- FLUSH PRIVILEGES;

-- Tables are auto-created by Spring Boot JPA (ddl-auto=update)
-- This script is for manual setup or reference only.

-- ── STUDENTS ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS students (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  reference_number  VARCHAR(20) NOT NULL UNIQUE,
  full_name         VARCHAR(100) NOT NULL,
  course            VARCHAR(100) NOT NULL,
  level             INT NOT NULL,
  email             VARCHAR(150) NOT NULL,
  active            BOOLEAN NOT NULL DEFAULT TRUE,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  last_login        DATETIME,
  INDEX idx_ref (reference_number),
  INDEX idx_course_level (course, level)
);

-- ── LECTURERS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lecturers (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50) NOT NULL UNIQUE,
  password      VARCHAR(255) NOT NULL,
  full_name     VARCHAR(100) NOT NULL,
  department    VARCHAR(100) NOT NULL,
  email         VARCHAR(150) NOT NULL,
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  last_login    DATETIME
);

-- ── QUIZZES ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quizzes (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  title             VARCHAR(200) NOT NULL,
  course            VARCHAR(100) NOT NULL,
  level             INT NOT NULL,
  duration_minutes  INT NOT NULL,
  scheduled_start   DATETIME NOT NULL,
  scheduled_end     DATETIME,
  active            BOOLEAN NOT NULL DEFAULT TRUE,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  created_by        VARCHAR(100),
  INDEX idx_course_level (course, level)
);

-- ── QUESTIONS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS questions (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  quiz_id         BIGINT NOT NULL,
  question_text   TEXT NOT NULL,
  option_a        VARCHAR(500) NOT NULL,
  option_b        VARCHAR(500) NOT NULL,
  option_c        VARCHAR(500) NOT NULL,
  option_d        VARCHAR(500) NOT NULL,
  correct_answer  VARCHAR(1) NOT NULL,
  marks           INT DEFAULT 1,
  order_index     INT,
  FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

-- ── QUIZ ATTEMPTS ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS quiz_attempts (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id          BIGINT NOT NULL,
  quiz_id             BIGINT NOT NULL,
  score               INT,
  total_marks         INT,
  percentage          DOUBLE,
  status              ENUM('IN_PROGRESS','SUBMITTED','AUTO_SUBMITTED','FLAGGED') DEFAULT 'IN_PROGRESS',
  cheating_score      INT DEFAULT 0,
  flagged_for_review  BOOLEAN DEFAULT FALSE,
  started_at          DATETIME,
  submitted_at        DATETIME,
  recording_path      VARCHAR(500),
  recording_url       VARCHAR(500),
  FOREIGN KEY (student_id) REFERENCES students(id),
  FOREIGN KEY (quiz_id) REFERENCES quizzes(id),
  UNIQUE KEY uniq_student_quiz (student_id, quiz_id),
  INDEX idx_quiz (quiz_id),
  INDEX idx_student (student_id),
  INDEX idx_flagged (flagged_for_review)
);

-- ── STUDENT ANSWERS ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS student_answers (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id       BIGINT NOT NULL,
  question_id      BIGINT NOT NULL,
  selected_answer  VARCHAR(1),
  is_correct       BOOLEAN,
  marks_awarded    INT DEFAULT 0,
  FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES questions(id)
);

-- ── CHEATING EVENTS ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cheating_events (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id       BIGINT NOT NULL,
  event_type       ENUM('NO_FACE_DETECTED','MULTIPLE_FACES','LOOKING_AWAY','PHONE_DETECTED',
                        'TAB_SWITCH','FULLSCREEN_EXIT','SCREENSHOT_DETECTED',
                        'COPY_PASTE_ATTEMPT','DEVTOOLS_OPEN','ESC_PRESSED') NOT NULL,
  description      VARCHAR(500),
  severity_points  INT,
  occurred_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  snapshot_path    VARCHAR(500),
  FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE,
  INDEX idx_attempt (attempt_id)
);

-- ── ANNOUNCEMENTS (added in v2.0) ─────────────────────────────
CREATE TABLE IF NOT EXISTS announcements (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  title           VARCHAR(200) NOT NULL,
  body            TEXT NOT NULL,
  target_course   VARCHAR(100),
  target_level    INT,
  lecturer_id     BIGINT,
  lecturer_name   VARCHAR(100),
  posted_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  pinned          BOOLEAN DEFAULT FALSE,
  type            ENUM('GENERAL','QUIZ_INSTRUCTION','RESULT_NOTICE','URGENT') DEFAULT 'GENERAL',
  INDEX idx_ann_course (target_course),
  INDEX idx_ann_level (target_level)
);

-- ── QUIZ ENROLLMENTS (added in v2.0) ──────────────────────────
CREATE TABLE IF NOT EXISTS quiz_enrollments (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id  BIGINT NOT NULL,
  quiz_id     BIGINT NOT NULL,
  enrolled_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  status      ENUM('ENROLLED','COMPLETED','ABSENT') DEFAULT 'ENROLLED',
  UNIQUE KEY uniq_enroll (student_id, quiz_id),
  FOREIGN KEY (student_id) REFERENCES students(id),
  FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
);

-- ── AI EXPLANATIONS (added in v2.0) ───────────────────────────
CREATE TABLE IF NOT EXISTS ai_explanations (
  id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id            BIGINT NOT NULL,
  question_id           BIGINT NOT NULL,
  correct_explanation   TEXT,
  wrong_explanation     TEXT,
  student_answer        VARCHAR(1),
  correct_answer        VARCHAR(1),
  was_correct           BOOLEAN,
  generated_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES questions(id),
  UNIQUE KEY uniq_exp (attempt_id, question_id)
);

-- ── ALTER TABLES: Add new columns to students ─────────────────
-- Note: Run this only if the columns don't already exist
-- Check first with: DESCRIBE students;

ALTER TABLE students
  ADD COLUMN program VARCHAR(200) AFTER full_name,
  ADD COLUMN department VARCHAR(100) AFTER program,
  ADD COLUMN enrollment_date DATETIME DEFAULT CURRENT_TIMESTAMP;

-- ── SAMPLE DATA (optional) ────────────────────────────────────
-- The DataSeeder.java class auto-inserts demo data on first run.
-- You can also run this manually:

/*
INSERT INTO students (reference_number, full_name, course, level, email) VALUES
  ('UMAT/CS/21/0042', 'Ama Korantema',  'Computer Science',      300, 'ama@umat.edu.gh'),
  ('UMAT/CS/21/0043', 'Kojo Asante',    'Computer Science',      300, 'kojo@umat.edu.gh'),
  ('UMAT/ME/22/0010', 'Fatima Hassan',  'Mining Engineering',    200, 'fatima@umat.edu.gh'),
  ('UMAT/EE/20/0055', 'Emeka Eze',      'Electrical Engineering', 400, 'emeka@umat.edu.gh');

-- Lecturer: password = umat2026 (BCrypt)
INSERT INTO lecturers (username, password, full_name, department, email) VALUES
  ('lecturer', '\$2a\$10$xK7K6R5cZQf9J9A4rD2E8.n9J3uT8jL2kpO1mH4nB7vC5wX0yZ3se',
   'Dr. Emmanuel Boateng', 'Computer Science', 'e.boateng@umat.edu.gh');
*/

SELECT 'UMAT Quiz Database setup complete!' AS status;
