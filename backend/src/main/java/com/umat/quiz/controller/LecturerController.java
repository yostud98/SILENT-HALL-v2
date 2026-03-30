package com.umat.quiz.controller;

import com.umat.quiz.model.*;
import com.umat.quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lecturer")
@CrossOrigin(origins = "*")
public class LecturerController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private QuizRepository quizRepo;
    @Autowired private QuizAttemptRepository attemptRepo;
    @Autowired private CheatingEventRepository cheatingRepo;
    @Autowired private LecturerRepository lecturerRepo;
    @Autowired private QuizEnrollmentRepository enrollmentRepo;
    @Autowired private AnnouncementRepository announcementRepo;
    @Autowired private QuestionRepository questionRepo;

    // ── Dashboard stats ───────────────────────────────────────
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        long totalStudents = studentRepo.count();
        long totalQuizzes = quizRepo.count();
        long totalAttempts = attemptRepo.count();
        long flagged = attemptRepo.findByFlaggedForReview(true).size();
        long announcements = announcementRepo.count();

        double avgScore = attemptRepo.findAll().stream()
            .filter(a -> a.getPercentage() != null)
            .mapToDouble(QuizAttempt::getPercentage)
            .average().orElse(0);

        return ResponseEntity.ok(Map.of(
            "totalStudents", totalStudents,
            "totalQuizzes", totalQuizzes,
            "totalAttempts", totalAttempts,
            "flaggedAttempts", flagged,
            "averageScore", Math.round(avgScore * 10.0) / 10.0,
            "announcements", announcements
        ));
    }

    // ── Students ──────────────────────────────────────────────
    @GetMapping("/students")
    public ResponseEntity<?> getStudents(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) Integer level) {
        List<Student> students;
        if (course != null && level != null) students = studentRepo.findByCourseAndLevel(course, level);
        else if (course != null) students = studentRepo.findByCourse(course);
        else if (level != null) students = studentRepo.findByLevel(level);
        else students = studentRepo.findAll();
        return ResponseEntity.ok(students);
    }

    // ── Scores for a quiz ─────────────────────────────────────
    @GetMapping("/scores/{quizId}")
    public ResponseEntity<?> getScores(@PathVariable Long quizId) {
        return ResponseEntity.ok(attemptRepo.findByQuizId(quizId).stream().map(a -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("attemptId", a.getId());
            row.put("studentName", a.getStudent().getFullName());
            row.put("referenceNumber", a.getStudent().getReferenceNumber());
            row.put("program", a.getStudent().getProgram());
            row.put("course", a.getStudent().getCourse());
            row.put("level", a.getStudent().getLevel());
            row.put("score", a.getScore());
            row.put("totalMarks", a.getTotalMarks());
            row.put("percentage", a.getPercentage());
            row.put("status", a.getStatus());
            row.put("cheatingScore", a.getCheatingScore());
            row.put("flagged", a.getFlaggedForReview());
            row.put("submittedAt", a.getSubmittedAt());
            row.put("recordingUrl", a.getRecordingUrl());
            row.put("recordingPath", a.getRecordingPath());
            return row;
        }).collect(Collectors.toList()));
    }

    // ── All attempts ──────────────────────────────────────────
    @GetMapping("/attempts")
    public ResponseEntity<?> getAttempts(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) Integer level) {
        List<QuizAttempt> attempts;
        if (course != null && level != null) attempts = attemptRepo.findByCourseAndLevel(course, level);
        else attempts = attemptRepo.findAll();
        return ResponseEntity.ok(attempts.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("attemptId", a.getId());
            m.put("studentName", a.getStudent().getFullName());
            m.put("referenceNumber", a.getStudent().getReferenceNumber());
            m.put("quizTitle", a.getQuiz().getTitle());
            m.put("score", a.getScore());
            m.put("percentage", a.getPercentage());
            m.put("cheatingScore", a.getCheatingScore());
            m.put("flagged", a.getFlaggedForReview());
            m.put("status", a.getStatus());
            m.put("submittedAt", a.getSubmittedAt());
            m.put("recordingUrl", a.getRecordingUrl());
            return m;
        }).collect(Collectors.toList()));
    }

    // ── Flagged ───────────────────────────────────────────────
    @GetMapping("/flagged")
    public ResponseEntity<?> getFlagged() {
        return ResponseEntity.ok(attemptRepo.findByFlaggedForReview(true));
    }

    // ── Cheating events for attempt ───────────────────────────
    @GetMapping("/cheating/{attemptId}")
    public ResponseEntity<?> getCheatingEvents(@PathVariable Long attemptId) {
        return ResponseEntity.ok(cheatingRepo.findByAttemptIdOrderByOccurredAtDesc(attemptId));
    }

    // ── All recordings (attempts with recording URLs) ─────────
    @GetMapping("/recordings")
    public ResponseEntity<?> getRecordings() {
        return ResponseEntity.ok(attemptRepo.findAll().stream()
            .filter(a -> a.getRecordingUrl() != null && !a.getRecordingUrl().isBlank())
            .map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("attemptId", a.getId());
                m.put("studentName", a.getStudent().getFullName());
                m.put("referenceNumber", a.getStudent().getReferenceNumber());
                m.put("course", a.getStudent().getCourse());
                m.put("quizTitle", a.getQuiz().getTitle());
                m.put("submittedAt", a.getSubmittedAt());
                m.put("cheatingScore", a.getCheatingScore());
                m.put("flagged", a.getFlaggedForReview());
                m.put("recordingUrl", a.getRecordingUrl());
                return m;
            }).collect(Collectors.toList()));
    }

    // ── Create / Schedule Quiz ────────────────────────────────
    @PostMapping("/quiz/create")
    public ResponseEntity<?> createQuiz(@RequestBody Map<String, Object> body) {
        Quiz quiz = new Quiz();
        quiz.setTitle(body.get("title").toString());
        quiz.setCourse(body.get("course").toString());
        quiz.setLevel(Integer.parseInt(body.get("level").toString()));
        quiz.setDurationMinutes(Integer.parseInt(body.get("durationMinutes").toString()));
        quiz.setCreatedBy(body.getOrDefault("createdBy", "Lecturer").toString());
        quiz.setDescription(body.getOrDefault("description", "").toString());
        quiz.setEnrollmentRequired(Boolean.parseBoolean(body.getOrDefault("enrollmentRequired", true).toString()));
        quiz.setActive(true);

        // Parse scheduled start/end
        quiz.setScheduledStart(LocalDateTime.parse(body.get("scheduledStart").toString()));
        if (body.containsKey("scheduledEnd") && body.get("scheduledEnd") != null
                && !body.get("scheduledEnd").toString().isBlank()) {
            quiz.setScheduledEnd(LocalDateTime.parse(body.get("scheduledEnd").toString()));
        }

        Quiz saved = quizRepo.save(quiz);

        // Add questions if provided
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = body.containsKey("questions")
            ? (List<Map<String, Object>>) body.get("questions") : List.of();

        int idx = 0;
        for (Map<String, Object> qData : questions) {
            Question q = new Question();
            q.setQuiz(saved);
            q.setQuestionText(qData.get("questionText").toString());
            q.setOptionA(qData.get("optionA").toString());
            q.setOptionB(qData.get("optionB").toString());
            q.setOptionC(qData.get("optionC").toString());
            q.setOptionD(qData.get("optionD").toString());
            q.setCorrectAnswer(qData.get("correctAnswer").toString().toUpperCase());
            q.setMarks(Integer.parseInt(qData.getOrDefault("marks", 1).toString()));
            q.setOrderIndex(idx++);
            questionRepo.save(q);
        }

        return ResponseEntity.ok(Map.of("id", saved.getId(), "title", saved.getTitle(),
            "message", "Quiz created successfully"));
    }

    // ── Score distribution for chart ──────────────────────────
    @GetMapping("/chart/distribution/{quizId}")
    public ResponseEntity<?> getScoreDistribution(@PathVariable Long quizId) {
        int[] buckets = new int[5];
        attemptRepo.findByQuizId(quizId).stream()
            .filter(a -> a.getPercentage() != null)
            .forEach(a -> { int idx = Math.min(4, (int)(a.getPercentage() / 20)); buckets[idx]++; });
        return ResponseEntity.ok(Map.of(
            "labels", new String[]{"0-19%","20-39%","40-59%","60-79%","80-100%"},
            "data", buckets
        ));
    }

    // ── All quizzes list ──────────────────────────────────────
    @GetMapping("/quizzes")
    public ResponseEntity<?> getAllQuizzes() {
        return ResponseEntity.ok(quizRepo.findAll().stream().map(q -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("title", q.getTitle());
            m.put("course", q.getCourse());
            m.put("level", q.getLevel());
            m.put("durationMinutes", q.getDurationMinutes());
            m.put("scheduledStart", q.getScheduledStart());
            m.put("scheduledEnd", q.getScheduledEnd());
            m.put("active", q.isActive());
            m.put("enrollmentRequired", q.isEnrollmentRequired());
            m.put("questionCount", q.getQuestions() != null ? q.getQuestions().size() : 0);
            m.put("enrollments", enrollmentRepo.findByQuizId(q.getId()).size());
            return m;
        }).collect(Collectors.toList()));
    }
}
