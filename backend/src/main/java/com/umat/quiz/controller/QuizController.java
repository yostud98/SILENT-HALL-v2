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
@RequestMapping("/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    @Autowired private QuizRepository quizRepo;
    @Autowired private QuizAttemptRepository attemptRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private CheatingEventRepository cheatingRepo;
    @Autowired private QuizEnrollmentRepository enrollmentRepo;
    @Autowired private AIExplanationRepository explanationRepo;
    @Autowired private StudentAnswerRepository answerRepo;

    // ── Get quizzes with status for a student ─────────────────
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableQuizzes(
            @RequestParam String course,
            @RequestParam Integer level,
            @RequestParam(required = false) Long studentId) {

        List<Quiz> quizzes = quizRepo.findByCourseAndLevel(course, level);
        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> result = quizzes.stream()
            .filter(Quiz::isActive)
            .map(q -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", q.getId());
                row.put("title", q.getTitle());
                row.put("course", q.getCourse());
                row.put("level", q.getLevel());
                row.put("durationMinutes", q.getDurationMinutes());
                row.put("scheduledStart", q.getScheduledStart());
                row.put("scheduledEnd", q.getScheduledEnd());
                row.put("description", q.getDescription() != null ? q.getDescription() : "");
                row.put("enrollmentRequired", q.isEnrollmentRequired());
                row.put("questionCount", q.getQuestions() != null ? q.getQuestions().size() : 0);

                boolean isLive = now.isAfter(q.getScheduledStart()) &&
                    (q.getScheduledEnd() == null || now.isBefore(q.getScheduledEnd()));
                boolean isUpcoming = now.isBefore(q.getScheduledStart());
                row.put("status", isLive ? "LIVE" : isUpcoming ? "UPCOMING" : "EXPIRED");
                row.put("isLive", isLive);
                row.put("isUpcoming", isUpcoming);

                if (studentId != null) {
                    row.put("enrolled", enrollmentRepo.existsByStudentIdAndQuizId(studentId, q.getId()));
                    row.put("attempted", attemptRepo.findByStudentIdAndQuizId(studentId, q.getId()).isPresent());
                } else {
                    row.put("enrolled", false);
                    row.put("attempted", false);
                }
                return row;
            }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── Enroll in a quiz ──────────────────────────────────────
    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@RequestBody Map<String, Long> body) {
        Long studentId = body.get("studentId");
        Long quizId = body.get("quizId");

        if (enrollmentRepo.existsByStudentIdAndQuizId(studentId, quizId)) {
            return ResponseEntity.ok(Map.of("message", "Already enrolled", "enrolled", true));
        }

        Quiz quiz = quizRepo.findById(quizId).orElseThrow();
        Student student = studentRepo.findById(studentId).orElseThrow();

        if (quiz.getScheduledEnd() != null && LocalDateTime.now().isAfter(quiz.getScheduledEnd())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Quiz has already ended."));
        }

        QuizEnrollment en = new QuizEnrollment();
        en.setStudent(student);
        en.setQuiz(quiz);
        enrollmentRepo.save(en);

        return ResponseEntity.ok(Map.of("message", "Enrolled successfully", "enrolled", true));
    }

    // ── Start a quiz ──────────────────────────────────────────
    @PostMapping("/start")
    public ResponseEntity<?> startQuiz(@RequestBody Map<String, Long> body) {
        Long studentId = body.get("studentId");
        Long quizId = body.get("quizId");

        Quiz quiz = quizRepo.findById(quizId).orElseThrow();
        Student student = studentRepo.findById(studentId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(quiz.getScheduledStart()))
            return ResponseEntity.badRequest().body(Map.of("error", "Quiz has not started yet."));
        if (quiz.getScheduledEnd() != null && now.isAfter(quiz.getScheduledEnd()))
            return ResponseEntity.badRequest().body(Map.of("error", "Quiz has ended."));
        if (quiz.isEnrollmentRequired() && !enrollmentRepo.existsByStudentIdAndQuizId(studentId, quizId))
            return ResponseEntity.status(403).body(Map.of("error", "You must enroll in this quiz first."));

        Optional<QuizAttempt> existing = attemptRepo.findByStudentIdAndQuizId(studentId, quizId);
        if (existing.isPresent()) {
            QuizAttempt ex = existing.get();
            if (ex.getStatus() != QuizAttempt.AttemptStatus.IN_PROGRESS)
                return ResponseEntity.badRequest().body(Map.of("error", "Quiz already submitted."));
            return buildStartResponse(ex, quiz);
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        attempt.setQuiz(quiz);
        attempt.setStartedAt(now);
        attempt.setStatus(QuizAttempt.AttemptStatus.IN_PROGRESS);
        attemptRepo.save(attempt);
        return buildStartResponse(attempt, quiz);
    }

    private ResponseEntity<?> buildStartResponse(QuizAttempt attempt, Quiz quiz) {
        List<Question> questions = new ArrayList<>(quiz.getQuestions());
        Collections.shuffle(questions);
        return ResponseEntity.ok(Map.of(
            "attemptId", attempt.getId(),
            "quizTitle", quiz.getTitle(),
            "description", quiz.getDescription() != null ? quiz.getDescription() : "",
            "durationMinutes", quiz.getDurationMinutes(),
            "totalQuestions", questions.size(),
            "questions", questions.stream().map(q -> Map.of(
                "id", q.getId(),
                "questionText", q.getQuestionText(),
                "optionA", q.getOptionA(),
                "optionB", q.getOptionB(),
                "optionC", q.getOptionC(),
                "optionD", q.getOptionD(),
                "marks", q.getMarks()
            )).toList()
        ));
    }

    // ── Submit quiz ───────────────────────────────────────────
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody Map<String, Object> body) {
        Long attemptId = Long.parseLong(body.get("attemptId").toString());
        boolean autoSubmit = Boolean.parseBoolean(body.getOrDefault("autoSubmit", false).toString());

        @SuppressWarnings("unchecked")
        Map<String, String> answers = (Map<String, String>) body.get("answers");

        QuizAttempt attempt = attemptRepo.findById(attemptId).orElseThrow();
        if (attempt.getStatus() != QuizAttempt.AttemptStatus.IN_PROGRESS)
            return ResponseEntity.badRequest().body(Map.of("error", "Already submitted."));

        int score = 0, total = 0;
        List<StudentAnswer> studentAnswers = new ArrayList<>();

        for (Question q : attempt.getQuiz().getQuestions()) {
            total += q.getMarks();
            String selected = answers != null ? answers.get(q.getId().toString()) : null;
            boolean correct = selected != null && selected.equalsIgnoreCase(q.getCorrectAnswer());
            StudentAnswer sa = new StudentAnswer();
            sa.setAttempt(attempt); sa.setQuestion(q);
            sa.setSelectedAnswer(selected); sa.setIsCorrect(correct);
            sa.setMarksAwarded(correct ? q.getMarks() : 0);
            studentAnswers.add(sa);
            if (correct) score += q.getMarks();
        }

        attempt.setScore(score);
        attempt.setTotalMarks(total);
        attempt.setPercentage(total > 0 ? (double) score / total * 100 : 0);
        attempt.setStatus(autoSubmit ? QuizAttempt.AttemptStatus.AUTO_SUBMITTED : QuizAttempt.AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setAnswers(studentAnswers);
        if (attempt.getCheatingScore() > 30) attempt.setFlaggedForReview(true);
        attemptRepo.save(attempt);

        enrollmentRepo.findByStudentIdAndQuizId(attempt.getStudent().getId(), attempt.getQuiz().getId())
            .ifPresent(e -> { e.setStatus(QuizEnrollment.EnrollmentStatus.COMPLETED); enrollmentRepo.save(e); });

        return ResponseEntity.ok(Map.of(
            "attemptId", attemptId, "score", score, "totalMarks", total,
            "percentage", attempt.getPercentage(), "passed", attempt.getPercentage() >= 50,
            "cheatingScore", attempt.getCheatingScore(), "flagged", attempt.getFlaggedForReview()
        ));
    }

    // ── My results with explanations ──────────────────────────
    @GetMapping("/my-results/{studentId}")
    public ResponseEntity<?> getMyResults(@PathVariable Long studentId) {
        List<QuizAttempt> attempts = attemptRepo.findByStudentId(studentId);
        return ResponseEntity.ok(attempts.stream()
            .filter(a -> a.getStatus() != QuizAttempt.AttemptStatus.IN_PROGRESS)
            .map(a -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("attemptId", a.getId());
                row.put("quizTitle", a.getQuiz().getTitle());
                row.put("course", a.getQuiz().getCourse());
                row.put("level", a.getQuiz().getLevel());
                row.put("score", a.getScore());
                row.put("totalMarks", a.getTotalMarks());
                row.put("percentage", a.getPercentage());
                row.put("passed", a.getPercentage() != null && a.getPercentage() >= 50);
                row.put("submittedAt", a.getSubmittedAt());
                row.put("cheatingScore", a.getCheatingScore());

                List<Map<String, Object>> qds = new ArrayList<>();
                if (a.getAnswers() != null) {
                    for (StudentAnswer sa : a.getAnswers()) {
                        Map<String, Object> qd = new LinkedHashMap<>();
                        qd.put("questionId", sa.getQuestion().getId());
                        qd.put("questionText", sa.getQuestion().getQuestionText());
                        qd.put("optionA", sa.getQuestion().getOptionA());
                        qd.put("optionB", sa.getQuestion().getOptionB());
                        qd.put("optionC", sa.getQuestion().getOptionC());
                        qd.put("optionD", sa.getQuestion().getOptionD());
                        qd.put("correctAnswer", sa.getQuestion().getCorrectAnswer());
                        qd.put("studentAnswer", sa.getSelectedAnswer());
                        qd.put("isCorrect", sa.getIsCorrect());
                        qd.put("marksAwarded", sa.getMarksAwarded());
                        qd.put("marks", sa.getQuestion().getMarks());
                        explanationRepo.findByAttemptIdAndQuestionId(a.getId(), sa.getQuestion().getId())
                            .ifPresent(exp -> {
                                qd.put("correctExplanation", exp.getCorrectExplanation());
                                qd.put("wrongExplanation", exp.getWrongExplanation());
                            });
                        qds.add(qd);
                    }
                }
                row.put("questions", qds);
                return row;
            }).collect(Collectors.toList()));
    }

    // ── Save AI explanations ──────────────────────────────────
    @PostMapping("/save-explanations")
    public ResponseEntity<?> saveExplanations(@RequestBody Map<String, Object> body) {
        Long attemptId = Long.parseLong(body.get("attemptId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> explanations = (List<Map<String, Object>>) body.get("explanations");
        QuizAttempt attempt = attemptRepo.findById(attemptId).orElseThrow();

        int saved = 0;
        for (Map<String, Object> exp : explanations) {
            Long questionId = Long.parseLong(exp.get("questionId").toString());
            if (explanationRepo.findByAttemptIdAndQuestionId(attemptId, questionId).isPresent()) continue;
            Question question = attempt.getQuiz().getQuestions().stream()
                .filter(q -> q.getId().equals(questionId)).findFirst().orElse(null);
            if (question == null) continue;

            AIExplanation ex = new AIExplanation();
            ex.setAttempt(attempt); ex.setQuestion(question);
            ex.setCorrectExplanation((String) exp.get("correctExplanation"));
            ex.setWrongExplanation((String) exp.get("wrongExplanation"));
            ex.setStudentAnswer((String) exp.get("studentAnswer"));
            ex.setCorrectAnswer((String) exp.get("correctAnswer"));
            ex.setWasCorrect(Boolean.parseBoolean(exp.getOrDefault("wasCorrect", false).toString()));
            explanationRepo.save(ex);
            saved++;
        }
        return ResponseEntity.ok(Map.of("saved", saved));
    }

    // ── Cheat event ───────────────────────────────────────────
    @PostMapping("/cheat-event")
    public ResponseEntity<?> recordCheatingEvent(@RequestBody Map<String, Object> body) {
        Long attemptId = Long.parseLong(body.get("attemptId").toString());
        QuizAttempt attempt = attemptRepo.findById(attemptId).orElseThrow();
        CheatingEvent event = new CheatingEvent();
        event.setAttempt(attempt);
        event.setEventType(CheatingEvent.EventType.valueOf(body.get("eventType").toString()));
        event.setDescription(body.getOrDefault("description", "").toString());
        int sev = switch (event.getEventType()) {
            case PHONE_DETECTED -> 15; case MULTIPLE_FACES -> 20; case SCREENSHOT_DETECTED -> 25;
            case NO_FACE_DETECTED -> 5; case LOOKING_AWAY -> 3; case TAB_SWITCH -> 10;
            case FULLSCREEN_EXIT -> 10; case DEVTOOLS_OPEN -> 20; default -> 5;
        };
        event.setSeverityPoints(sev);
        cheatingRepo.save(event);
        int newScore = Math.min(100, attempt.getCheatingScore() + sev);
        attempt.setCheatingScore(newScore);
        if (newScore > 30) attempt.setFlaggedForReview(true);
        attemptRepo.save(attempt);
        return ResponseEntity.ok(Map.of("cheatingScore", newScore));
    }
}
