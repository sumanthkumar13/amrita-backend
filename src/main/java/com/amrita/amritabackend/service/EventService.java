package com.amrita.amritabackend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amrita.amritabackend.dto.EventResponseDto;
import com.amrita.amritabackend.model.Event;
import com.amrita.amritabackend.model.EventInvitee;
import com.amrita.amritabackend.model.User;
import com.amrita.amritabackend.repository.EventInviteeRepository;
import com.amrita.amritabackend.repository.LikeRepository;
import com.amrita.amritabackend.repository.UserRepository;

@Service
public class EventService {

    @Autowired
    private EventInviteeRepository eventInviteeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRepository likeRepository; // ✅ Needed for likedByMe

    // ✅ Convert Event → EventResponseDto
    public EventResponseDto mapToDto(Event event, Long userId) {
        User organiser = event.getOrganiser();

        // Check if explicitly invited
        boolean invitedExplicitly = eventInviteeRepository.existsByEventAndUser(
                event,
                userRepository.findById(userId).orElseThrow());

        // Check if liked by current user
        boolean likedByMe = likeRepository.findByUserIdAndEventId(userId, event.getId()).isPresent();

        // Build organiser map
        Map<String, Object> organiserMap = new HashMap<>();
        organiserMap.put("id", organiser.getId());
        organiserMap.put("fullName", organiser.getFullName());
        organiserMap.put("email", organiser.getEmail());
        organiserMap.put("branch", organiser.getBranch());
        organiserMap.put("rollNumber", organiser.getRollNumber());
        organiserMap.put("phoneNumber", organiser.getPhoneNumber());

        return EventResponseDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .venue(event.getVenue())
                .dateTime(event.getDateTime())
                .imageUrl(event.getImageUrl())
                .likesCount(event.getLikesCount())
                .createdAt(event.getCreatedAt())
                .expiresAt(event.getExpiresAt())
                .visibilityType(event.getVisibilityType())
                .organiser(organiserMap)
                .invitedExplicitly(invitedExplicitly)
                .likedByMe(likedByMe) // ✅ new field
                .build();
    }

    // ✅ Convert List<Event> → List<EventResponseDto>
    public List<EventResponseDto> mapToDtoList(List<Event> events, Long userId) {
        return events.stream().map(e -> mapToDto(e, userId)).toList();
    }

    // ✅ Detailed mapping (with invitees list)
    // ✅ Detailed mapping (with invitees list)
    public Map<String, Object> mapToDetailedDto(Event event) {
        Map<String, Object> data = new HashMap<>();
        data.put("event", event);

        // Fetch invitees for USERS or GROUPS visibility
        if ("USERS".equalsIgnoreCase(event.getVisibilityType())
                || "GROUPS".equalsIgnoreCase(event.getVisibilityType())) {

            List<EventInvitee> invitees = eventInviteeRepository.findByEvent(event);

            List<Map<String, Object>> inviteeDetails = invitees.stream().map(inv -> {
                User u = inv.getUser();
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("fullName", u.getFullName());
                m.put("email", u.getEmail());
                m.put("branch", u.getBranch());
                m.put("rollNumber", u.getRollNumber());
                m.put("phoneNumber", u.getPhoneNumber());
                return m;
            }).toList();

            data.put("invitees", inviteeDetails);
        }

        return data;
    }

}
