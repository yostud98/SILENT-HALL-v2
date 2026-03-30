package com.umat.quiz.repository;

import com.umat.quiz.model.QuizEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizEnrollmentRepository extends JpaRepository<QuizEnrollment, Long> {
    Optional<QuizEnrollment> findByStudentIdAndQuizId(Long studentId, Long quizId);
    boolean existsByStudentIdAndQuizId(Long studentId, Long quizId);
    List<QuizEnrollment> findByStudentId(Long studentId);
    List<QuizEnrollment> findByQuizId(Long quizId);
    long countByStudentId(Long studentId);
}
