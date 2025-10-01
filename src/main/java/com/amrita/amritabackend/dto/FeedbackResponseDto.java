package com.amrita.amritabackend.dto;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponseDto {
    private Long id;
    private Long eventId;
    private String content;
    private boolean anonymous;
    private Instant createdAt;

    private Map<String, Object> sender; // If anonymous = null / {"name":"Anonymous"}
    private String reply; // organiser’s reply
}
