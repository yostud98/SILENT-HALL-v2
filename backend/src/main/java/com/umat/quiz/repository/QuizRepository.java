package com.umat.quiz.repository;

import com.umat.quiz.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByCourseAndLevel(String course, Integer level);
    List<Quiz> findByCourse(String course);
    List<Quiz> findByLevel(Integer level);
    List<Quiz> findByActive(boolean active);
}
