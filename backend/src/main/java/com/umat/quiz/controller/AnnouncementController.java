package com.umat.quiz.controller;

import com.umat.quiz.model.Announcement;
import com.umat.quiz.model.Lecturer;
import com.umat.quiz.repository.AnnouncementRepository;
import com.umat.quiz.repository.LecturerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/announcements")
@CrossOrigin(origins = "*")
public class AnnouncementController {

    @Autowired private AnnouncementRepository announcementRepo;
    @Autowired private LecturerRepository lecturerRepo;

    // ── Student: get announcements for their course/level ─────
    @GetMapping("/student")
    public ResponseEntity<?> getForStudent(
            @RequestParam String course,
            @RequestParam Integer level) {
        return ResponseEntity.ok(announcementRepo.findForStudent(course, level));
    }

    // ── Lecturer: get all announcements ──────────────────────
    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(announcementRepo.findAllByOrderByPinnedDescPostedAtDesc());
    }

    // ── Lecturer: post new announcement ──────────────────────
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Announcement ann = new Announcement();
        ann.setTitle(body.get("title").toString());
        ann.setBody(body.get("body").toString());
        ann.setLecturerName(body.getOrDefault("lecturerName", "Lecturer").toString());

        if (body.containsKey("targetCourse") && body.get("targetCourse") != null
                && !body.get("targetCourse").toString().isBlank()) {
            ann.setTargetCourse(body.get("targetCourse").toString());
        }
        if (body.containsKey("targetLevel") && body.get("targetLevel") != null) {
            try { ann.setTargetLevel(Integer.parseInt(body.get("targetLevel").toString())); }
            catch (NumberFormatException ignored) {}
        }
        if (body.containsKey("pinned"))
            ann.setPinned(Boolean.parseBoolean(body.get("pinned").toString()));
        if (body.containsKey("type")) {
            try { ann.setType(Announcement.AnnouncementType.valueOf(body.get("type").toString())); }
            catch (IllegalArgumentException ignored) {}
        }

        Announcement saved = announcementRepo.save(ann);
        return ResponseEntity.ok(saved);
    }

    // ── Lecturer: delete announcement ─────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        announcementRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }
}
