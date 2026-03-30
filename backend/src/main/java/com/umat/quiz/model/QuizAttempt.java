package com.umat.quiz.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quiz_attempts")
@Data
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    private Integer score;
    private Integer totalMarks;
    private Double percentage;

    @Enumerated(EnumType.STRING)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    private Integer cheatingScore = 0; // 0–100
    private Boolean flaggedForReview = false;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    private String recordingPath; // local or S3 path
    private String recordingUrl;  // presigned URL or direct URL

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
    private List<StudentAnswer> answers;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
    private List<CheatingEvent> cheatingEvents;

    public enum AttemptStatus {
        IN_PROGRESS, SUBMITTED, AUTO_SUBMITTED, FLAGGED
    }
}
