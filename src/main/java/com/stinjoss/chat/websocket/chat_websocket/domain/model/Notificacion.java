package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {
    private Long id;
    private Long usuarioId;
    private TipoNotificacion tipo;
    private String mensaje;
    private boolean leida;
    private LocalDateTime fechaCreacion;
}
