package com.amrita.amritabackend.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventResponseDto {
    private Long id;
    private String title;
    private String description;
    private String venue;
    private Instant dateTime;
    private String imageUrl;
    private int likesCount;
    private Instant createdAt;
    private Instant expiresAt;
    private String visibilityType;

    private Map<String, Object> organiser; // minimal user info
    private List<Map<String, Object>> invitees; // simplified invitee info
    private boolean invitedExplicitly; // flag for current user
    private boolean likedByMe; // ✅ new field

}
