package com.stinjoss.chat.websocket.chat_websocket.infrastructure.rest.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private String path;
    private Map<String, String> errors; // Para errores de validación (campo -> mensaje)
}
