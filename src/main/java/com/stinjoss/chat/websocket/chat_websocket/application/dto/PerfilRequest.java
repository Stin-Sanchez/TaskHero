package com.stinjoss.chat.websocket.chat_websocket.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PerfilRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String nombre;
    
    private String passwordActual;
    
    @Size(min = 6)
    private String passwordNueva;

    private String avatarUrl;
}
