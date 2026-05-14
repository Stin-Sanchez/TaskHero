package com.stinjoss.chat.websocket.chat_websocket.domain.repository;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import java.util.List;
import java.util.Optional;

public interface TareaRepository {
    Optional<Tarea> findById(Long id);
    List<Tarea> findByUsuarioId(Long usuarioId);
    Tarea save(Tarea tarea);
    void deleteById(Long id);
}
