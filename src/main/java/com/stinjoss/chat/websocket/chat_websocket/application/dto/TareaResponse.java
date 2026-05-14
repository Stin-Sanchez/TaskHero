package com.stinjoss.chat.websocket.chat_websocket.application.dto;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TareaResponse {
    private Long id;
    private String titulo;
    private String descripcion;
    private String prioridad;
    private LocalDate fechaLimite;
    private boolean completada;
    private String categoria;

    public static TareaResponse fromDomain(Tarea tarea) {
        return TareaResponse.builder()
                .id(tarea.getId())
                .titulo(tarea.getTitulo())
                .descripcion(tarea.getDescripcion())
                .prioridad(tarea.getPrioridad() != null ? tarea.getPrioridad().name() : null)
                .fechaLimite(tarea.getFechaLimite())
                .completada(tarea.isCompletada())
                .categoria(tarea.getCategoria())
                .build();
    }
}
