package com.amrita.amritabackend.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.amrita.amritabackend.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Use Instant since Event.dateTime is Instant
    List<Event> findByDateTimeBetween(Instant start, Instant end);

    List<Event> findByDateTime(Instant dateTime);

    // ✅ Safer method: convenience wrapper for LocalDate
    default List<Event> findByDate(LocalDate date) {
        Instant start = date.atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toInstant();
        return findByDateTimeBetween(start, end);
    }

    List<Event> findByOrganiser_Id(Long userId);

    List<Event> findByOrganiserId(Long organiserId);

    // ✅ Only return events visible to this user (non-paged)
    @Query("""
            SELECT e FROM Event e
            WHERE e.visibilityType = 'PUBLIC'
               OR e.organiser.id = :userId
               OR EXISTS (
                   SELECT 1 FROM EventInvitee ei
                   WHERE ei.event = e AND ei.user.id = :userId
               )
            """)
    List<Event> findVisibleEventsForUser(@Param("userId") Long userId);

    // ✅ Visible upcoming events (today → +2 days)
    @Query("""
            SELECT e FROM Event e
            WHERE (e.visibilityType = 'PUBLIC'
                   OR e.organiser.id = :userId
                   OR EXISTS (
                       SELECT 1 FROM EventInvitee ei
                       WHERE ei.event = e AND ei.user.id = :userId
                   ))
              AND e.dateTime >= :start
              AND e.dateTime < :end
            """)
    List<Event> findUpcomingVisibleEventsForUser(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    // ✅ Only non-expired events
    @Query("SELECT e FROM Event e WHERE e.expiresAt > CURRENT_TIMESTAMP")
    Page<Event> findVisibleEvents(Pageable pageable);

    Page<Event> findAll(Pageable pageable);

    // ✅ Paged version of visible events
    @Query("""
            SELECT e FROM Event e
            WHERE e.visibilityType = 'PUBLIC'
               OR e.organiser.id = :userId
               OR EXISTS (
                   SELECT 1 FROM EventInvitee ei
                   WHERE ei.event = e AND ei.user.id = :userId
               )
            """)
    Page<Event> findVisibleEventsForUser(@Param("userId") Long userId, Pageable pageable);
}
