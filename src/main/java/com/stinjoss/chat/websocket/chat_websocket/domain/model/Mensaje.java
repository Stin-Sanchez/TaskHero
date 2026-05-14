package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {
    private Long id;
    private Long remitenteId;
    private String remitenteNombre;
    private Long receptorId; // Nulo si es grupal
    private Long grupoId;    // Nulo si es privado 1:1
    private String contenido;
    private LocalDateTime timestamp;
}
