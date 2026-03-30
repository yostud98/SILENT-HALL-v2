package com.umat.quiz.repository;

import com.umat.quiz.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a WHERE " +
           "(a.targetCourse IS NULL OR a.targetCourse = :course) AND " +
           "(a.targetLevel IS NULL OR a.targetLevel = :level) " +
           "ORDER BY a.pinned DESC, a.postedAt DESC")
    List<Announcement> findForStudent(String course, Integer level);

    List<Announcement> findAllByOrderByPinnedDescPostedAtDesc();
}
