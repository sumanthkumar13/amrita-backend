package com.amrita.amritabackend.dto;

import java.util.List;

import lombok.Data;

@Data
public class CreateEventRequest {
    @Data
    public static class OrganiserDto {
        private Long id;
    }

    private String title;
    private String description;
    private String venue;
    private String dateTime;
    private String imageUrl;
    private String visibilityType;
    private OrganiserDto organiser;
    private List<Long> inviteeUserIds;

    private List<Long> inviteeGroupIds;
}
