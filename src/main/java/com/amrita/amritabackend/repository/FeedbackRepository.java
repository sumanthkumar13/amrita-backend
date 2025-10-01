package com.amrita.amritabackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.amrita.amritabackend.model.Feedback;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // All feedback for a specific event
    List<Feedback> findByEvent_Id(Long eventId);

    // All feedback given by a specific user
    List<Feedback> findByUser_Id(Long userId);
}
