package com.stinjoss.chat.websocket.chat_websocket.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogroResponse {
    private String codigo;
    private String nombre;
    private String descripcion;
    private String icono;
    private boolean desbloqueado;
    private LocalDateTime fechaObtenido;
}
