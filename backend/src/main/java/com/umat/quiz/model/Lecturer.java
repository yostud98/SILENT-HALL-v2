package com.umat.quiz.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecturers")
@Data
public class Lecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt hashed

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String email;

    private String passwordResetToken;

    private LocalDateTime passwordResetTokenExpiry;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLogin;
}
