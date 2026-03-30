package com.umat.quiz.controller;

import com.umat.quiz.model.*;
import com.umat.quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Silent Hall — Result Controller
 * ---------------------------------
 * Dedicated endpoints for the student results dashboard.
 *
 * GET  /api/results/{studentId}          → list of all attempts (summary)
 * GET  /api/results/details/{attemptId}  → full attempt with all question answers
 */
@RestController
@RequestMapping("/results")
@CrossOrigin(origins = "*")
public class ResultController {

    @Autowired private QuizAttemptRepository attemptRepo;
    @Autowired private AIExplanationRepository explanationRepo;

    // ── GET /api/results/{studentId} ─────────────────────────────
    // Returns a list of all submitted attempts for a student (summary view).
    @GetMapping("/{studentId}")
    public ResponseEntity<?> getResults(@PathVariable Long studentId) {
        List<QuizAttempt> attempts = attemptRepo.findByStudentId(studentId);

        List<Map<String, Object>> summary = attempts.stream()
                .filter(a -> a.getStatus() != QuizAttempt.AttemptStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(
                        a -> a.getSubmittedAt() != null ? a.getSubmittedAt() : a.getStartedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("attemptId",   a.getId());
                    row.put("quizTitle",   a.getQuiz().getTitle());
                    row.put("course",      a.getQuiz().getCourse());
                    row.put("level",       a.getQuiz().getLevel());
                    row.put("score",       a.getScore());
                    row.put("totalMarks",  a.getTotalMarks());
                    row.put("percentage",  a.getPercentage());
                    row.put("passed",      a.getPercentage() != null && a.getPercentage() >= 50);
                    row.put("submittedAt", a.getSubmittedAt());
                    row.put("startedAt",   a.getStartedAt());
                    row.put("cheatingScore", a.getCheatingScore());
                    row.put("flagged",     a.getFlaggedForReview());
                    row.put("status",      a.getStatus().name());
                    return row;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(summary);
    }

    // ── GET /api/results/details/{attemptId} ─────────────────────
    // Returns the full attempt detail including all student answers and any
    // previously generated AI explanations.
    @GetMapping("/details/{attemptId}")
    public ResponseEntity<?> getResultDetails(@PathVariable Long attemptId) {
        QuizAttempt attempt = attemptRepo.findById(attemptId).orElse(null);
        if (attempt == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("attemptId",    attempt.getId());
        detail.put("quizTitle",    attempt.getQuiz().getTitle());
        detail.put("course",       attempt.getQuiz().getCourse());
        detail.put("level",        attempt.getQuiz().getLevel());
        detail.put("score",        attempt.getScore());
        detail.put("totalMarks",   attempt.getTotalMarks());
        detail.put("percentage",   attempt.getPercentage());
        detail.put("passed",       attempt.getPercentage() != null && attempt.getPercentage() >= 50);
        detail.put("submittedAt",  attempt.getSubmittedAt());
        detail.put("startedAt",    attempt.getStartedAt());
        detail.put("cheatingScore", attempt.getCheatingScore());
        detail.put("flagged",      attempt.getFlaggedForReview());
        detail.put("status",       attempt.getStatus().name());

        // Build per-question breakdown
        List<Map<String, Object>> questions = new ArrayList<>();
        if (attempt.getAnswers() != null) {
            for (StudentAnswer sa : attempt.getAnswers()) {
                Map<String, Object> qd = new LinkedHashMap<>();
                qd.put("questionId",    sa.getQuestion().getId());
                qd.put("questionText",  sa.getQuestion().getQuestionText());
                qd.put("optionA",       sa.getQuestion().getOptionA());
                qd.put("optionB",       sa.getQuestion().getOptionB());
                qd.put("optionC",       sa.getQuestion().getOptionC());
                qd.put("optionD",       sa.getQuestion().getOptionD());
                qd.put("correctAnswer", sa.getQuestion().getCorrectAnswer());
                qd.put("studentAnswer", sa.getSelectedAnswer());
                qd.put("isCorrect",     sa.getIsCorrect());
                qd.put("marksAwarded",  sa.getMarksAwarded());
                qd.put("marks",         sa.getQuestion().getMarks());

                // Attach any previously saved AI explanation
                explanationRepo
                        .findByAttemptIdAndQuestionId(attempt.getId(), sa.getQuestion().getId())
                        .ifPresent(exp -> {
                            qd.put("correctExplanation", exp.getCorrectExplanation());
                            qd.put("wrongExplanation",   exp.getWrongExplanation());
                        });

                questions.add(qd);
            }
        }
        detail.put("questions", questions);

        return ResponseEntity.ok(detail);
    }
}
