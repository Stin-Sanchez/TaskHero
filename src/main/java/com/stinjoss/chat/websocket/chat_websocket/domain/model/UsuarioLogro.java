package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.CodigoLogro;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioLogro {
    private Long id;
    private Long usuarioId;
    private CodigoLogro codigo;
    private LocalDateTime fechaObtenido;
}
