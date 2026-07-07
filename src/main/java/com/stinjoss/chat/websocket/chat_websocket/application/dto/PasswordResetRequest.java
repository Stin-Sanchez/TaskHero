package com.stinjoss.chat.websocket.chat_websocket.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @Email
    @NotBlank
    private String email;
    
    @NotBlank
    private String token;
    
    @NotBlank
    private String nuevaPassword;
}
