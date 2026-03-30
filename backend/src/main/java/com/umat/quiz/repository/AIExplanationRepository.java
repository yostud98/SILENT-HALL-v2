package com.umat.quiz.repository;

import com.umat.quiz.model.AIExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AIExplanationRepository extends JpaRepository<AIExplanation, Long> {
    List<AIExplanation> findByAttemptId(Long attemptId);
    Optional<AIExplanation> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
