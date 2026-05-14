package com.stinjoss.chat.websocket.chat_websocket.application.dto;

import lombok.Data;
import java.util.List;

@Data
public class GrupoRequest {
    private String nombre;
    private List<Long> miembrosIds;
}
