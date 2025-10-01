package com.amrita.amritabackend.model;

import java.time.Instant;

import jakarta.persistence.Column;
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
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;
    private String venue;

    @ManyToOne
    @JoinColumn(name = "organiser_id", nullable = false)
    private User organiser;

    @Column(nullable = false)
    private Instant dateTime; // always stored in UTC

    private String imageUrl;

    @Column(columnDefinition = "int default 0")
    private int likesCount;

    private Instant createdAt;
    private Instant expiresAt;

    // ✅ New fields for private visibility
    @Column(nullable = false)
    private String visibilityType; // PUBLIC | USERS

    private String visibilityValue; // optional: e.g. branch/year/group name
}
