package com.umat.quiz.controller;

import com.umat.quiz.model.Student;
import com.umat.quiz.model.Lecturer;
import com.umat.quiz.repository.StudentRepository;
import com.umat.quiz.repository.LecturerRepository;
import com.umat.quiz.repository.QuizAttemptRepository;
import com.umat.quiz.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Pattern REF_PATTERN =
        Pattern.compile("^UMAT/[A-Z]{2,4}/\\d{2}/\\d{4}$", Pattern.CASE_INSENSITIVE);

    @Autowired private StudentRepository studentRepo;
    @Autowired private LecturerRepository lecturerRepo;
    @Autowired private QuizAttemptRepository attemptRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    // ── Student Login ─────────────────────────────────────────
    @PostMapping("/student/login")
    public ResponseEntity<?> studentLogin(@RequestBody Map<String, String> body) {
        String refNumber = body.getOrDefault("referenceNumber", "").trim().toUpperCase();
        String password = body.getOrDefault("password", "");

        if (!REF_PATTERN.matcher(refNumber).matches()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid reference number format. Expected: UMAT/CS/21/0042"
            ));
        }

        Optional<Student> opt = studentRepo.findByReferenceNumber(refNumber);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "Reference number not found. Please sign up first."
            ));
        }

        Student student = opt.get();
        
        // If password is provided, verify it (new user-password login)
        if (!password.isEmpty()) {
            if (student.getPassword() == null || !passwordEncoder.matches(password, student.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid password."));
            }
        }
        
        if (!student.isActive()) {
            return ResponseEntity.status(403).body(Map.of("error", "Account deactivated."));
        }

        student.setLastLogin(LocalDateTime.now());
        studentRepo.save(student);

        long totalQuizzes = attemptRepo.findByStudentId(student.getId()).stream()
            .filter(a -> a.getStatus() != com.umat.quiz.model.QuizAttempt.AttemptStatus.IN_PROGRESS)
            .count();
        student.setTotalQuizzesTaken((int) totalQuizzes);

        String token = jwtUtil.generateStudentToken(student);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "student", buildStudentProfile(student, (int) totalQuizzes)
        ));
    }

    // ── Get student profile ───────────────────────────────────
    @GetMapping("/student/profile/{id}")
    public ResponseEntity<?> getStudentProfile(@PathVariable Long id) {
        Student student = studentRepo.findById(id).orElseThrow();
        long totalQuizzes = attemptRepo.findByStudentId(id).stream()
            .filter(a -> a.getStatus() != com.umat.quiz.model.QuizAttempt.AttemptStatus.IN_PROGRESS)
            .count();
        return ResponseEntity.ok(buildStudentProfile(student, (int) totalQuizzes));
    }

    private Map<String, Object> buildStudentProfile(Student student, int totalQuizzes) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", student.getId());
        profile.put("referenceNumber", student.getReferenceNumber());
        profile.put("fullName", student.getFullName());
        profile.put("program", student.getProgram());
        profile.put("department", student.getDepartment());
        profile.put("course", student.getCourse());
        profile.put("level", student.getLevel());
        profile.put("email", student.getEmail());
        profile.put("enrollmentDate", student.getEnrollmentDate());
        profile.put("totalQuizzesTaken", totalQuizzes);
        profile.put("lastLogin", student.getLastLogin());
        return profile;
    }

    // ── Lecturer Login ────────────────────────────────────────
    @PostMapping("/lecturer/login")
    public ResponseEntity<?> lecturerLogin(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");

        Optional<Lecturer> opt = lecturerRepo.findByUsername(username);
        if (opt.isEmpty() || !passwordEncoder.matches(password, opt.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials."));
        }

        Lecturer lecturer = opt.get();
        lecturer.setLastLogin(LocalDateTime.now());
        lecturerRepo.save(lecturer);

        return ResponseEntity.ok(Map.of(
            "token", jwtUtil.generateLecturerToken(lecturer),
            "lecturer", Map.of(
                "id", lecturer.getId(),
                "fullName", lecturer.getFullName(),
                "department", lecturer.getDepartment(),
                "email", lecturer.getEmail(),
                "username", lecturer.getUsername()
            )
        ));
    }

    // ── Lecturer: change password ─────────────────────────────
    @PostMapping("/lecturer/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String oldPw = body.getOrDefault("oldPassword", "");
        String newPw = body.getOrDefault("newPassword", "");

        if (newPw.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters."));

        Optional<Lecturer> opt = lecturerRepo.findByUsername(username);
        if (opt.isEmpty() || !passwordEncoder.matches(oldPw, opt.get().getPassword()))
            return ResponseEntity.status(401).body(Map.of("error", "Current password is incorrect."));

        Lecturer lec = opt.get();
        lec.setPassword(passwordEncoder.encode(newPw));
        lecturerRepo.save(lec);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    // ── Lecturer: update profile ──────────────────────────────
    @PutMapping("/lecturer/profile/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Lecturer lec = lecturerRepo.findById(id).orElseThrow();
        if (body.containsKey("fullName")) lec.setFullName(body.get("fullName"));
        if (body.containsKey("department")) lec.setDepartment(body.get("department"));
        if (body.containsKey("email")) lec.setEmail(body.get("email"));
        lecturerRepo.save(lec);
        return ResponseEntity.ok(Map.of("message", "Profile updated."));
    }

    // ── Lecturer: Signup ──────────────────────────────────────
    @PostMapping("/lecturer/signup")
    public ResponseEntity<?> lecturerSignup(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        String fullName = body.getOrDefault("fullName", "").trim();
        String email = body.getOrDefault("email", "").trim();
        String department = body.getOrDefault("department", "").trim();

        // Validation
        if (username.length() < 3) return ResponseEntity.badRequest().body(Map.of("error", "Username must be at least 3 characters."));
        if (password.length() < 6) return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters."));
        if (fullName.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Full name is required."));
        if (email.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        if (department.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Department is required."));

        // Check if username already exists
        if (lecturerRepo.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists."));
        }

        // Create new lecturer
        Lecturer lec = new Lecturer();
        lec.setUsername(username);
        lec.setPassword(passwordEncoder.encode(password));
        lec.setFullName(fullName);
        lec.setEmail(email);
        lec.setDepartment(department);
        lecturerRepo.save(lec);

        return ResponseEntity.ok(Map.of(
            "message", "Lecturer account created successfully. Please log in.",
            "redirect", "login.html?role=lecturer"
        ));
    }

    // ── Student: Signup ───────────────────────────────────────
    @PostMapping("/student/signup")
    public ResponseEntity<?> studentSignup(@RequestBody Map<String, String> body) {
        String referenceNumber = body.getOrDefault("referenceNumber", "").trim().toUpperCase();
        String password = body.getOrDefault("password", "");
        String fullName = body.getOrDefault("fullName", "").trim();
        String email = body.getOrDefault("email", "").trim();

        // Validation
        if (!REF_PATTERN.matcher(referenceNumber).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid reference number format: UMAT/CS/21/0042"));
        }
        if (password.length() < 6) return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters."));
        if (fullName.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Full name is required."));
        if (email.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));

        // Check if reference number already has account
        Optional<Student> existing = studentRepo.findByReferenceNumber(referenceNumber);
        if (existing.isPresent() && existing.get().getPassword() != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "This reference number already has an account."));
        }

        Student student;
        if (existing.isPresent()) {
            // Update existing student from seeder with password
            student = existing.get();
            student.setPassword(passwordEncoder.encode(password));
            student.setEmail(email);
            student.setFullName(fullName);
        } else {
            // Create new student (if allowed)
            student = new Student();
            student.setReferenceNumber(referenceNumber);
            student.setPassword(passwordEncoder.encode(password));
            student.setFullName(fullName);
            student.setEmail(email);
            student.setDepartment("General");
            student.setProgram("General");
            student.setCourse("General");
            student.setLevel(100);
        }
        
        studentRepo.save(student);
        return ResponseEntity.ok(Map.of(
            "message", "Student account created successfully. Please log in.",
            "redirect", "login.html?role=student"
        ));
    }

    // ── Forgot Password: Student ──────────────────────────────
    @PostMapping("/student/forgot-password")
    public ResponseEntity<?> studentForgotPassword(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim();
        
        Optional<Student> opt = studentRepo.findAll().stream()
            .filter(s -> s.getEmail() != null && s.getEmail().equals(email))
            .findFirst();

        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "If email exists, a reset link will be sent."));
        }

        Student student = opt.get();
        String resetToken = UUID.randomUUID().toString();
        student.setPasswordResetToken(resetToken);
        student.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        studentRepo.save(student);

        return ResponseEntity.ok(Map.of(
            "message", "Password reset token generated.",
            "resetToken", resetToken  // In production, send via email
        ));
    }

    // ── Reset Password: Student ───────────────────────────────
    @PostMapping("/student/reset-password")
    public ResponseEntity<?> studentResetPassword(@RequestBody Map<String, String> body) {
        String resetToken = body.getOrDefault("resetToken", "");
        String newPassword = body.getOrDefault("newPassword", "");

        if (newPassword.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters."));

        Optional<Student> opt = studentRepo.findAll().stream()
            .filter(s -> s.getPasswordResetToken() != null && s.getPasswordResetToken().equals(resetToken))
            .findFirst();

        if (opt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid reset token."));
        }

        Student student = opt.get();
        if (student.getPasswordResetTokenExpiry() == null || student.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body(Map.of("error", "Reset token has expired."));
        }

        student.setPassword(passwordEncoder.encode(newPassword));
        student.setPasswordResetToken(null);
        student.setPasswordResetTokenExpiry(null);
        studentRepo.save(student);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please log in."));
    }

    // ── Forgot Password: Lecturer ─────────────────────────────
    @PostMapping("/lecturer/forgot-password")
    public ResponseEntity<?> lecturerForgotPassword(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim();

        Optional<Lecturer> opt = lecturerRepo.findAll().stream()
            .filter(l -> l.getEmail() != null && l.getEmail().equals(email))
            .findFirst();

        if (opt.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "If email exists, a reset link will be sent."));
        }

        Lecturer lecturer = opt.get();
        String resetToken = UUID.randomUUID().toString();
        lecturer.setPasswordResetToken(resetToken);
        lecturer.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        lecturerRepo.save(lecturer);

        return ResponseEntity.ok(Map.of(
            "message", "Password reset token generated.",
            "resetToken", resetToken  // In production, send via email
        ));
    }

    // ── Reset Password: Lecturer ──────────────────────────────
    @PostMapping("/lecturer/reset-password")
    public ResponseEntity<?> lecturerResetPassword(@RequestBody Map<String, String> body) {
        String resetToken = body.getOrDefault("resetToken", "");
        String newPassword = body.getOrDefault("newPassword", "");

        if (newPassword.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters."));

        Optional<Lecturer> opt = lecturerRepo.findAll().stream()
            .filter(l -> l.getPasswordResetToken() != null && l.getPasswordResetToken().equals(resetToken))
            .findFirst();

        if (opt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid reset token."));
        }

        Lecturer lecturer = opt.get();
        if (lecturer.getPasswordResetTokenExpiry() == null || lecturer.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(401).body(Map.of("error", "Reset token has expired."));
        }

        lecturer.setPassword(passwordEncoder.encode(newPassword));
        lecturer.setPasswordResetToken(null);
        lecturer.setPasswordResetTokenExpiry(null);
        lecturerRepo.save(lecturer);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please log in."));
    }
}
