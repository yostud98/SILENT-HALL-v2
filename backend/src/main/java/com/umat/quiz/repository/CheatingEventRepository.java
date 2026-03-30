package com.umat.quiz.repository;

import com.umat.quiz.model.CheatingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CheatingEventRepository extends JpaRepository<CheatingEvent, Long> {
    List<CheatingEvent> findByAttemptId(Long attemptId);
    List<CheatingEvent> findByAttemptIdOrderByOccurredAtDesc(Long attemptId);
}
