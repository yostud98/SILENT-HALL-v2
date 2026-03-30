package com.umat.quiz.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "cheating_events")
@Data
public class CheatingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    private String description;
    private Integer severityPoints; // points added to cheating score
    private LocalDateTime occurredAt = LocalDateTime.now();
    private String snapshotPath; // screenshot at time of event

    public enum EventType {
        NO_FACE_DETECTED,
        MULTIPLE_FACES,
        LOOKING_AWAY,
        PHONE_DETECTED,
        TAB_SWITCH,
        FULLSCREEN_EXIT,
        SCREENSHOT_DETECTED,
        COPY_PASTE_ATTEMPT,
        DEVTOOLS_OPEN,
        ESC_PRESSED
    }
}
