package com.amrita.amritabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse {
    private String status; // success / error
    private String message; // response message
    private Object data; // extra info (can be null)
}
