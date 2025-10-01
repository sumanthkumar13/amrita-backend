package com.amrita.amritabackend.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
    private String fullName;
    private String branch;
    private String rollNumber;
    private String phoneNumber;
    private Integer batchYear; // ✅ changed to Integer
}
