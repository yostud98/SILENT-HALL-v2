package com.umat.quiz.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 25)
    private String referenceNumber;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String program;      // Full program name e.g. "Computer Science and Engineering"

    @Column(nullable = false)
    private String department;   // Department/faculty e.g. "Computer Science"

    @Column(nullable = false)
    private String course;       // Short name used in quiz matching

    @Column(nullable = false)
    private Integer level;       // 100, 200, 300, 400

    @Column(nullable = false)
    private String email;

    private String phoneNumber;

    private String password;  // BCrypt hashed (optional for reference-based login)

    private String passwordResetToken;

    private LocalDateTime passwordResetTokenExpiry;

    private LocalDateTime enrollmentDate = LocalDateTime.now();

    @Column(nullable = false)
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastLogin;

    @Transient
    private Integer totalQuizzesTaken = 0;
}
