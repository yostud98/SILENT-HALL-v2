package com.umat.quiz.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "student_answers")
@Data
public class StudentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    private String selectedAnswer; // "A", "B", "C", "D" or null
    private Boolean isCorrect;
    private Integer marksAwarded;
}
