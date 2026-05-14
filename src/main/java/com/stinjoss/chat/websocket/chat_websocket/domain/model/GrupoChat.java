package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrupoChat {
    private Long id;
    private String nombre;
    private List<Long> miembrosIds;
}
