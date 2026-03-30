package com.umat.quiz.controller;

import com.umat.quiz.model.QuizAttempt;
import com.umat.quiz.repository.QuizAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/recording")
@CrossOrigin(origins = "*")
public class RecordingController {

    @Value("${recordings.local.path:./recordings}")
    private String recordingsPath;

    @Autowired private QuizAttemptRepository attemptRepo;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadRecording(
            @RequestParam("file") MultipartFile file,
            @RequestParam("attemptId") Long attemptId,
            @RequestParam("referenceNumber") String refNumber,
            @RequestParam("course") String course) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        try {
            // Ensure recordings directory exists
            Path dir = Paths.get(recordingsPath);
            Files.createDirectories(dir);

            // Generate structured filename: reference_course_timestamp.mp4
            String timestamp = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeRef = refNumber.replaceAll("[^a-zA-Z0-9]", "_");
            String safeCourse = course.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = safeRef + "_" + safeCourse + "_" + timestamp + ".mp4";

            Path filePath = dir.resolve(filename);
            file.transferTo(filePath.toFile());

            // Update attempt record
            QuizAttempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
            attempt.setRecordingPath(filePath.toAbsolutePath().toString());
            attempt.setRecordingUrl("/api/recording/stream/" + filename);
            attemptRepo.save(attempt);

            return ResponseEntity.ok(Map.of(
                "message", "Recording uploaded successfully",
                "filename", filename,
                "path", filePath.toAbsolutePath().toString()
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to save recording: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/stream/{filename}")
    public ResponseEntity<?> streamRecording(@PathVariable String filename) {
        // Basic security: prevent path traversal
        if (filename.contains("..") || filename.contains("/")) {
            return ResponseEntity.badRequest().build();
        }
        Path path = Paths.get(recordingsPath, filename);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return ResponseEntity.ok()
                .header("Content-Type", "video/mp4")
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
