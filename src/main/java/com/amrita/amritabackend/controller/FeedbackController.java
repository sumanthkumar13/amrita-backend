package com.amrita.amritabackend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amrita.amritabackend.dto.ApiResponse;
import com.amrita.amritabackend.dto.FeedbackRequest;
import com.amrita.amritabackend.dto.FeedbackResponseDto;
import com.amrita.amritabackend.service.FeedbackService;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    // ✅ Add new feedback
    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addFeedback(@RequestBody FeedbackRequest request) {
        FeedbackResponseDto dto = feedbackService.addFeedback(request);
        return ResponseEntity.ok(new ApiResponse("success", "Feedback submitted", dto));
    }

    // ✅ Get feedback for an event (organiser view)
    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse> getFeedbackForEvent(@PathVariable Long eventId) {
        List<FeedbackResponseDto> feedbacks = feedbackService.getFeedbackForEvent(eventId);
        return ResponseEntity.ok(new ApiResponse("success", "Feedbacks fetched", feedbacks));
    }

    // ✅ Get feedback by a user (sender view)
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getFeedbackByUser(@PathVariable Long userId) {
        List<FeedbackResponseDto> feedbacks = feedbackService.getFeedbackByUser(userId);
        return ResponseEntity.ok(new ApiResponse("success", "User feedback fetched", feedbacks));
    }

    // ✅ Add organiser reply
    @PostMapping("/{feedbackId}/reply")
    public ResponseEntity<ApiResponse> addReply(
            @PathVariable Long feedbackId,
            @RequestBody String reply) {
        FeedbackResponseDto dto = feedbackService.addReply(feedbackId, reply);
        return ResponseEntity.ok(new ApiResponse("success", "Reply saved", dto));
    }

    // ✅ Get single feedback by ID
    @GetMapping("/{feedbackId}")
    public ResponseEntity<ApiResponse> getFeedbackById(@PathVariable Long feedbackId) {
        return feedbackService.getFeedback(feedbackId)
                .map(dto -> ResponseEntity.ok(new ApiResponse("success", "Feedback fetched", dto)))
                .orElse(ResponseEntity.status(404)
                        .body(new ApiResponse("error", "Feedback not found", null)));
    }

}
