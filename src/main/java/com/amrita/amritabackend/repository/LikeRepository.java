package com.amrita.amritabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.amrita.amritabackend.model.Event;
import com.amrita.amritabackend.model.Like;
import com.amrita.amritabackend.model.User;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndEvent(User user, Event event);

    long countByEvent(Event event);

    void deleteByUserAndEvent(User user, Event event);

    // ✅ Add this for likedByMe check
    Optional<Like> findByUserIdAndEventId(Long userId, Long eventId);
}
