package com.umat.quiz.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_explanations")
@Data
public class AIExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String correctExplanation;   // Why the correct answer is correct

    @Column(columnDefinition = "TEXT")
    private String wrongExplanation;     // Why the student's answer was wrong (null if correct)

    private String studentAnswer;
    private String correctAnswer;
    private Boolean wasCorrect;

    private LocalDateTime generatedAt = LocalDateTime.now();
}
