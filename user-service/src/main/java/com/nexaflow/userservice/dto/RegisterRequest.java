package com.nexaflow.userservice.dto;

import com.nexaflow.userservice.model.UserRole;
import lombok.Data;

@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private UserRole role;
}
