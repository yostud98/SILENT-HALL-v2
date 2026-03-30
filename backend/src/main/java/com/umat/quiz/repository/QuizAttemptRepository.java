package com.umat.quiz.repository;

import com.umat.quiz.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByStudentId(Long studentId);
    List<QuizAttempt> findByQuizId(Long quizId);
    Optional<QuizAttempt> findByStudentIdAndQuizId(Long studentId, Long quizId);
    List<QuizAttempt> findByFlaggedForReview(boolean flagged);

    @Query("SELECT a FROM QuizAttempt a WHERE a.quiz.course = :course AND a.quiz.level = :level")
    List<QuizAttempt> findByCourseAndLevel(String course, Integer level);

    @Query("SELECT AVG(a.percentage) FROM QuizAttempt a WHERE a.quiz.id = :quizId")
    Double findAverageScoreByQuiz(Long quizId);
}
