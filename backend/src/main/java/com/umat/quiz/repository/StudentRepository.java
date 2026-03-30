package com.umat.quiz.repository;

import com.umat.quiz.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByReferenceNumber(String referenceNumber);
    List<Student> findByCourseAndLevel(String course, Integer level);
    List<Student> findByCourse(String course);
    List<Student> findByLevel(Integer level);
    boolean existsByReferenceNumber(String referenceNumber);
}
