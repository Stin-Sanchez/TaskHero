package com.stinjoss.chat.websocket.chat_websocket.domain.repository;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Notificacion;
import java.util.List;

public interface NotificacionRepository {
    Notificacion save(Notificacion notificacion);
    List<Notificacion> findByUsuarioId(Long usuarioId);
    void markAsRead(Long notificacionId);
}
