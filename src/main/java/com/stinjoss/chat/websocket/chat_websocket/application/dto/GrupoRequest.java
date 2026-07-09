package com.stinjoss.chat.websocket.chat_websocket.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class GrupoRequest {
    @NotBlank(message = "El nombre del gremio es obligatorio")
    private String nombre;
    private List<Long> miembrosIds;
}
