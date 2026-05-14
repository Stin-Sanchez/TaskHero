package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.Prioridad;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarea {
    private Long id;
    private Long usuarioId;
    private String titulo;
    private String descripcion;
    private Prioridad prioridad;
    private LocalDate fechaLimite;
    private boolean completada;
    private String categoria;

    public void completar() {
        this.completada = true;
    }
}
