package com.amrita.amritabackend.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amrita.amritabackend.dto.FeedbackRequest;
import com.amrita.amritabackend.dto.FeedbackResponseDto;
import com.amrita.amritabackend.model.Event;
import com.amrita.amritabackend.model.Feedback;
import com.amrita.amritabackend.model.User;
import com.amrita.amritabackend.repository.EventRepository;
import com.amrita.amritabackend.repository.FeedbackRepository;
import com.amrita.amritabackend.repository.UserRepository;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ Add feedback
    public FeedbackResponseDto addFeedback(FeedbackRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Always link feedback to user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Feedback feedback = new Feedback();
        feedback.setEvent(event);
        feedback.setUser(user); // ✅ Always set user
        feedback.setAnonymous(request.isAnonymous()); // just a flag
        feedback.setContent(request.getContent());
        feedback.setCreatedAt(Instant.now());

        Feedback saved = feedbackRepository.save(feedback);

        return mapToDto(saved);
    }

    // ✅ Get all feedback for an event
    public List<FeedbackResponseDto> getFeedbackForEvent(Long eventId) {
        return feedbackRepository.findByEvent_Id(eventId).stream()
                .map(this::mapToDto)
                .toList();
    }

    // ✅ Get all feedback given by a user
    public List<FeedbackResponseDto> getFeedbackByUser(Long userId) {
        return feedbackRepository.findByUser_Id(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    public Optional<FeedbackResponseDto> getFeedback(Long id) {
        return feedbackRepository.findById(id).map(this::mapToDto);
    }

    // ✅ Add reply by organiser
    public FeedbackResponseDto addReply(Long feedbackId, String reply) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        feedback.setReply(reply);
        Feedback saved = feedbackRepository.save(feedback);

        return mapToDto(saved);
    }

    // ✅ Mapper
    private FeedbackResponseDto mapToDto(Feedback feedback) {
        Map<String, Object> senderMap = new HashMap<>();
        if (feedback.isAnonymous() || feedback.getUser() == null) {
            senderMap.put("name", "Anonymous");
        } else {
            User u = feedback.getUser();
            senderMap.put("id", u.getId());
            senderMap.put("fullName", u.getFullName());
            senderMap.put("email", u.getEmail());
        }

        return FeedbackResponseDto.builder()
                .id(feedback.getId())
                .eventId(feedback.getEvent().getId())
                .content(feedback.getContent())
                .anonymous(feedback.isAnonymous())
                .createdAt(feedback.getCreatedAt())
                .sender(senderMap)
                .reply(feedback.getReply())
                .build();
    }
}
