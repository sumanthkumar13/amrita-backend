package com.amrita.amritabackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.amrita.amritabackend.model.Event;
import com.amrita.amritabackend.model.EventInvitee;
import com.amrita.amritabackend.model.User;

@Repository
public interface EventInviteeRepository extends JpaRepository<EventInvitee, Long> {

    List<EventInvitee> findByEvent(Event event);

    List<EventInvitee> findByUser(User user);

    boolean existsByEventAndUser(Event event, User user);

    @Query("SELECT ei.event.id FROM EventInvitee ei WHERE ei.user.id = :userId AND ei.event.id IN :eventIds")
    List<Long> findInvitedEventIdsForUser(@Param("userId") Long userId, @Param("eventIds") List<Long> eventIds);

}
