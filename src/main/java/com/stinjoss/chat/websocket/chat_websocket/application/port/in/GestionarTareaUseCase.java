package com.stinjoss.chat.websocket.chat_websocket.application.port.in;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.TareaRequest;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;

import java.util.List;

public interface GestionarTareaUseCase {
    Tarea crearTarea(Long usuarioId, TareaRequest request);
    Tarea actualizarTarea(Long usuarioId, Long tareaId, TareaRequest request);
    void eliminarTarea(Long usuarioId, Long tareaId);
    List<Tarea> listarTareasPorUsuario(Long usuarioId);
    Tarea marcarComoCompletada(Long usuarioId, Long tareaId);
}
