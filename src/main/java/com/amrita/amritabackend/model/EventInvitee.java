package com.amrita.amritabackend.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "event_invitees")
public class EventInvitee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Link to event
    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // ✅ Link to user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Instant invitedAt;
}
