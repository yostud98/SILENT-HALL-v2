package com.umat.quiz.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Data
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String course;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private LocalDateTime scheduledStart;

    private LocalDateTime scheduledEnd;

    @Column(nullable = false)
    private boolean active = true;

    private boolean enrollmentRequired = true;

    private String description;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Question> questions;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private String createdBy;
}
