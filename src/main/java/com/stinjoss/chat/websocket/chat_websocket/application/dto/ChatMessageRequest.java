package com.stinjoss.chat.websocket.chat_websocket.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private Long receptorId; // Nulo si es grupal
    private Long grupoId;    // Nulo si es privado
    private String contenido;
    private boolean escribiendo;
}
