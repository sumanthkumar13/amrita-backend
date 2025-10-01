package com.amrita.amritabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    private Long eventId; // Event for which feedback is given
    private Long userId; // Sender (null/ignored if anonymous)
    private String content; // Feedback text
    private boolean anonymous; // true = hide sender info
}
