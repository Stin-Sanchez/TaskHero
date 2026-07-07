package com.stinjoss.chat.websocket.chat_websocket.domain.repository;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Notificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;
import java.util.List;
import java.time.LocalDateTime;

public interface NotificacionRepository {
    Notificacion save(Notificacion notificacion);
    List<Notificacion> findByUsuarioId(Long usuarioId);
    void markAsRead(Long notificacionId);
    boolean existsByUsuarioIdAndTipoAndFechaCreacionAfter(Long usuarioId, TipoNotificacion tipo, LocalDateTime fecha);
}
