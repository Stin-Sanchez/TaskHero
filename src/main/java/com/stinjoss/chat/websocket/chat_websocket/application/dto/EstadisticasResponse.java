package com.stinjoss.chat.websocket.chat_websocket.application.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class EstadisticasResponse {
    private long totalTareas;
    private long completadas;
    private long pendientes;
    private double porcentajeCumplimiento;
    private Map<String, Long> tareasPorPrioridad;
    private Map<String, Long> tareasPorCategoria;
}
