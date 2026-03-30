package com.umat.quiz.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Data
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    private String targetCourse;   // null = all courses
    private Integer targetLevel;   // null = all levels

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id")
    private Lecturer postedBy;

    private String lecturerName;

    @Column(nullable = false)
    private LocalDateTime postedAt = LocalDateTime.now();

    private boolean pinned = false;

    @Enumerated(EnumType.STRING)
    private AnnouncementType type = AnnouncementType.GENERAL;

    public enum AnnouncementType {
        GENERAL, QUIZ_INSTRUCTION, RESULT_NOTICE, URGENT
    }
}
