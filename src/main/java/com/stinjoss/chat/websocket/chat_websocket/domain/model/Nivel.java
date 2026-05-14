package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nivel {
    private Integer numero;
    private Integer xpRequerido;
    private String funcionDesbloqueada;
    private String descripcion;
}
