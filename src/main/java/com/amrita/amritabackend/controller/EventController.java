package com.amrita.amritabackend.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amrita.amritabackend.dto.ApiResponse;
import com.amrita.amritabackend.dto.CreateEventRequest;
import com.amrita.amritabackend.dto.EventResponseDto;
import com.amrita.amritabackend.model.Event;
import com.amrita.amritabackend.model.EventInvitee;
import com.amrita.amritabackend.model.Group;
import com.amrita.amritabackend.model.GroupMember;
import com.amrita.amritabackend.model.Like;
import com.amrita.amritabackend.model.User;
import com.amrita.amritabackend.repository.EventInviteeRepository;
import com.amrita.amritabackend.repository.EventRepository;
import com.amrita.amritabackend.repository.GroupMemberRepository;
import com.amrita.amritabackend.repository.GroupRepository;
import com.amrita.amritabackend.repository.LikeRepository;
import com.amrita.amritabackend.repository.UserRepository;
import com.amrita.amritabackend.service.EventService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private EventInviteeRepository eventInviteeRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private EventService eventService;

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse> createEvent(@RequestBody CreateEventRequest request) {
        try {
            Long organiserId = request.getOrganiser().getId();
            String title = request.getTitle();
            String description = request.getDescription();
            String venue = request.getVenue();
            Instant dateTime = Instant.parse(request.getDateTime());
            String imageUrl = request.getImageUrl();
            String visibilityType = request.getVisibilityType() != null ? request.getVisibilityType() : "PUBLIC";

            User organiser = userRepository.findById(organiserId)
                    .orElseThrow(() -> new RuntimeException("Organiser not found"));

            Event event = Event.builder()
                    .title(title)
                    .description(description)
                    .venue(venue)
                    .organiser(organiser)
                    .dateTime(dateTime)
                    .imageUrl(imageUrl)
                    .createdAt(Instant.now())
                    .expiresAt(dateTime.plusSeconds(60L * 60 * 24 * 21)) // 21 days expiry
                    .visibilityType(visibilityType)
                    .build();

            Event savedEvent = eventRepository.save(event);

            // Handle invitees (same logic as before, but now clean since we have Lists)
            if ("USERS".equalsIgnoreCase(visibilityType) || "GROUPS".equalsIgnoreCase(visibilityType)) {
                Set<Long> finalInviteeIds = new HashSet<>();

                if (request.getInviteeUserIds() != null) {
                    finalInviteeIds.addAll(request.getInviteeUserIds());
                }

                if (request.getInviteeGroupIds() != null) {
                    for (Long gid : request.getInviteeGroupIds()) {
                        Group group = groupRepository.findById(gid)
                                .orElseThrow(() -> new RuntimeException("Group not found: " + gid));
                        List<GroupMember> members = groupMemberRepository.findByGroup(group);
                        for (GroupMember gm : members) {
                            if (gm.getUser() != null) {
                                finalInviteeIds.add(gm.getUser().getId());
                            }
                        }
                    }
                }

                finalInviteeIds.remove(organiser.getId());

                for (Long uid : finalInviteeIds) {
                    userRepository.findById(uid).ifPresent(invitee -> {
                        boolean exists = eventInviteeRepository.existsByEventAndUser(savedEvent, invitee);
                        if (!exists) {
                            EventInvitee ei = EventInvitee.builder()
                                    .event(savedEvent)
                                    .user(invitee)
                                    .invitedAt(Instant.now())
                                    .build();
                            eventInviteeRepository.save(ei);
                        }
                    });
                }
            }

            return ResponseEntity.ok(new ApiResponse("success", "Event created successfully", savedEvent));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("error", "Failed to create event: " + ex.getMessage(), null));
        }
    }

    // ✅ Like/Unlike toggle
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse> likeEvent(@PathVariable Long id, @RequestParam Long userId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return likeRepository.findByUserAndEvent(user, event)
                .map(existingLike -> {
                    likeRepository.delete(existingLike);
                    event.setLikesCount((int) likeRepository.countByEvent(event));
                    eventRepository.save(event);

                    Map<String, Object> data = new HashMap<>();
                    data.put("likesCount", event.getLikesCount());
                    data.put("likedByMe", false);

                    return ResponseEntity.ok(new ApiResponse("success", "Event unliked", data));
                })
                .orElseGet(() -> {
                    Like newLike = Like.builder().user(user).event(event).build();
                    likeRepository.save(newLike);
                    event.setLikesCount((int) likeRepository.countByEvent(event));
                    eventRepository.save(event);

                    Map<String, Object> data = new HashMap<>();
                    data.put("likesCount", event.getLikesCount());
                    data.put("likedByMe", true);

                    return ResponseEntity.ok(new ApiResponse("success", "Event liked", data));
                });
    }

    // ✅ Get today's events (with invitedExplicitly flag)
    @GetMapping("/today")
    public ResponseEntity<ApiResponse> getTodayEvents(@RequestParam Long userId) {
        LocalDate today = LocalDate.now(IST);
        ZonedDateTime start = today.atStartOfDay(IST);
        ZonedDateTime end = start.plusDays(1);

        List<Event> events = eventRepository.findVisibleEventsForUser(userId).stream()
                .filter(e -> !e.getDateTime().isBefore(start.toInstant()) &&
                        e.getDateTime().isBefore(end.toInstant()))
                .toList();

        List<EventResponseDto> responseEvents = eventService.mapToDtoList(events, userId);

        return ResponseEntity.ok(new ApiResponse("success", "Today's events fetched", responseEvents));
    }

    // ✅ Get events by date (with invitedExplicitly flag)
    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse> getEventsByDate(@PathVariable String date,
            @RequestParam Long userId) {
        LocalDate selectedDate = LocalDate.parse(date);
        ZonedDateTime start = selectedDate.atStartOfDay(IST);
        ZonedDateTime end = start.plusDays(1);

        List<Event> events = eventRepository.findVisibleEventsForUser(userId).stream()
                .filter(e -> !e.getDateTime().isBefore(start.toInstant()) &&
                        e.getDateTime().isBefore(end.toInstant()))
                .toList();

        List<EventResponseDto> responseEvents = eventService.mapToDtoList(events, userId);

        return ResponseEntity.ok(new ApiResponse("success", "Events fetched for " + date, responseEvents));
    }

    // ✅ Get detailed events for a month (with invitedExplicitly flag)
    // ✅ Get detailed events for a month (with invitedExplicitly flag)
    @GetMapping("/month/details/{yearMonth}")
    public ResponseEntity<ApiResponse> getEventsDetailsByMonth(
            @PathVariable String yearMonth,
            @RequestParam Long userId) {
        try {
            YearMonth ym = YearMonth.parse(yearMonth);
            ZonedDateTime start = ym.atDay(1).atStartOfDay(IST);
            ZonedDateTime end = ym.atEndOfMonth().plusDays(1).atStartOfDay(IST);

            // All visible events for this user in the month
            List<Event> events = eventRepository.findVisibleEventsForUser(userId).stream()
                    .filter(e -> !e.getDateTime().isBefore(start.toInstant()) &&
                            e.getDateTime().isBefore(end.toInstant()))
                    .toList();

            // Convert to DTOs
            List<EventResponseDto> responseEvents = eventService.mapToDtoList(events, userId);

            return ResponseEntity.ok(new ApiResponse("success",
                    "Monthly event details fetched", responseEvents));

        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("error", "Invalid yearMonth format. Expected YYYY-MM", null));
        }
    }

    // ✅ Get all events sorted by likes
    // ✅ Get all events sorted by likes (safe DTO response)
    // ✅ Get all events sorted by likes (returns DTOs)
    @GetMapping
    public ResponseEntity<ApiResponse> getAllEventsSortedByLikes(@RequestParam Long userId) {
        // ✅ Start from today midnight, not "now"
        ZonedDateTime todayStart = LocalDate.now(IST).atStartOfDay(IST);
        ZonedDateTime twoDaysLater = todayStart.plusDays(3); // today + 2 days

        List<Event> events = eventRepository.findUpcomingVisibleEventsForUser(
                userId,
                todayStart.toInstant(),
                twoDaysLater.toInstant()).stream()
                .sorted((e1, e2) -> Integer.compare(e2.getLikesCount(), e1.getLikesCount()))
                .toList();

        List<EventResponseDto> responseEvents = eventService.mapToDtoList(events, userId);

        return ResponseEntity.ok(new ApiResponse("success", "Upcoming top events fetched", responseEvents));
    }

    // ✅ Get events by user
    // ✅ Get events by user (safe DTO response)
    // ✅ Get events created by a user (returns EventResponseDto list)
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getEventsByUser(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("error", "User not found", null));
        }

        List<Event> events = eventRepository.findByOrganiser_Id(userId);

        // Convert all events → DTOs (organiser is the same user, so invitedExplicitly =
        // false always)
        List<EventResponseDto> responseEvents = events.stream()
                .map(e -> eventService.mapToDto(e, userId))
                .toList();

        return ResponseEntity.ok(new ApiResponse("success", "User's events fetched", responseEvents));
    }

    // ✅ Get event by ID (with invitees if USERS visibility)
    // ✅ Get event by ID with safe DTO response
    // ✅ Get event by ID (now returns EventResponseDto + invitees if USERS
    // visibility)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getEventById(
            @PathVariable Long id,
            @RequestParam Long userId) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Convert Event → DTO (with invitedExplicitly flag)
        EventResponseDto dto = eventService.mapToDto(event, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("event", dto);

        // If USERS visibility → fetch invitees (safe user info)
        // If USERS or GROUPS visibility → fetch invitees (safe user info)
        if ("USERS".equalsIgnoreCase(event.getVisibilityType())
                || "GROUPS".equalsIgnoreCase(event.getVisibilityType())) {

            List<EventInvitee> invitees = eventInviteeRepository.findByEvent(event);

            List<Map<String, Object>> inviteeDetails = invitees.stream()
                    .map(inv -> {
                        User u = inv.getUser();
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", u.getId());
                        m.put("fullName", u.getFullName());
                        m.put("email", u.getEmail());
                        m.put("branch", u.getBranch());
                        m.put("rollNumber", u.getRollNumber());
                        m.put("phoneNumber", u.getPhoneNumber());
                        return m;
                    })
                    .toList();

            data.put("invitees", inviteeDetails);
        }

        return ResponseEntity.ok(new ApiResponse("success", "Event fetched", data));
    }

    // ✅ Delete an event (only organiser can delete)
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse> deleteEvent(@PathVariable("eventId") Long eventId,
            @RequestParam("userId") Long userId) {
        return eventRepository.findById(eventId).map(event -> {
            if (!event.getOrganiser().getId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse("error", "You are not the organiser of this event", null));
            }

            try {
                if (event.getImageUrl() != null && event.getImageUrl().contains("res.cloudinary.com")) {
                    try {
                        String publicId = extractPublicId(event.getImageUrl());
                        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    } catch (Exception ce) {
                        System.err.println("⚠️ Cloudinary delete failed: " + ce.getMessage());
                    }
                }

                eventRepository.delete(event);
                return ResponseEntity.ok(new ApiResponse("success", "Event deleted successfully", null));

            } catch (Exception ex) {
                ex.printStackTrace();
                return ResponseEntity.internalServerError()
                        .body(new ApiResponse("error", "Failed to delete event: " + ex.getMessage(), null));
            }

        }).orElseGet(() -> ResponseEntity.status(404)
                .body(new ApiResponse("error", "Event not found", null)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateEvent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestParam Long userId) {
        try {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            // ✅ Only organiser can update
            if (!event.getOrganiser().getId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse("error", "You are not the organiser of this event", null));
            }

            // ✅ Update allowed fields
            if (payload.containsKey("title"))
                event.setTitle(payload.get("title").toString());
            if (payload.containsKey("description"))
                event.setDescription(payload.get("description").toString());
            if (payload.containsKey("venue"))
                event.setVenue(payload.get("venue").toString());
            if (payload.containsKey("dateTime")) {
                Instant newDateTime = Instant.parse(payload.get("dateTime").toString());
                event.setDateTime(newDateTime);
                event.setExpiresAt(newDateTime.plusSeconds(60L * 60 * 24 * 21));
            }

            // ✅ Poster re-upload → delete old first
            if (payload.containsKey("imageUrl")) {
                String newUrl = payload.get("imageUrl").toString();

                if (event.getImageUrl() != null
                        && event.getImageUrl().contains("res.cloudinary.com")
                        && !event.getImageUrl().equals(newUrl)) {
                    try {
                        String publicId = extractPublicId(event.getImageUrl());
                        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    } catch (Exception ce) {
                        System.err.println("⚠️ Cloudinary delete failed: " + ce.getMessage());
                    }
                }

                event.setImageUrl(newUrl);
            }

            Event updatedEvent = eventRepository.save(event);

            return ResponseEntity.ok(new ApiResponse("success", "Event updated successfully", updatedEvent));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("error", "Failed to update event: " + ex.getMessage(), null));
        }
    }

    // ✅ Paged events API (visibility-aware)
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse> getPagedEvents(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateTime,desc") String sort) {

        try {
            // Parse sorting string
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            String sortDirection = sortParams.length > 1 ? sortParams[1] : "asc";

            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    sortDirection.equalsIgnoreCase("desc")
                            ? Sort.by(sortField).descending()
                            : Sort.by(sortField).ascending());

            // Fetch paged visible events
            Page<Event> pagedEvents = eventRepository.findVisibleEventsForUser(userId, pageable);

            // Map to DTOs with invitedExplicitly flag
            List<EventResponseDto> dtoList = eventService.mapToDtoList(pagedEvents.getContent(), userId);

            // Wrap back into Page-like structure
            Map<String, Object> response = new HashMap<>();
            response.put("content", dtoList);
            response.put("pageable", pagedEvents.getPageable());
            response.put("totalPages", pagedEvents.getTotalPages());
            response.put("totalElements", pagedEvents.getTotalElements());
            response.put("number", pagedEvents.getNumber());
            response.put("size", pagedEvents.getSize());
            response.put("first", pagedEvents.isFirst());
            response.put("last", pagedEvents.isLast());
            response.put("numberOfElements", pagedEvents.getNumberOfElements());

            return ResponseEntity.ok(new ApiResponse("success", "Paged events fetched", response));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("error", "Failed to fetch paged events: " + ex.getMessage(), null));
        }
    }

    private String extractPublicId(String url) {
        String[] parts = url.split("/");
        String filename = parts[parts.length - 1];
        return filename.substring(0, filename.lastIndexOf("."));
    }
}
