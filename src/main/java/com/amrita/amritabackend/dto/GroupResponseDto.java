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
public class GroupResponseDto {
    private Long id;
    private String name;
    private String description;
    private String type;
    private Instant createdAt;

    // ✅ Minimal creator info (id + name + email is enough)
    private Map<String, Object> createdBy;

    // ✅ Each member: id + name + role
    private List<Map<String, Object>> members;
}
