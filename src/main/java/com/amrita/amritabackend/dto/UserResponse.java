package com.amrita.amritabackend.dto;

import com.amrita.amritabackend.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    // ✅ Convenience method to build from User entity
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .branch(user.getBranch())
                .rollNumber(user.getRollNumber())
                .phoneNumber(user.getPhoneNumber())
                .batchYear(user.getBatchYear())
                .build();
    }

    private Long id;
    private String email;
    private String fullName;
    private String branch;
    private String rollNumber;
    private String phoneNumber;

    private Integer batchYear;
}
