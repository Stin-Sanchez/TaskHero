package com.stinjoss.chat.websocket.chat_websocket.application.port.out;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Notificacion;

public interface NotificacionPushPort {
    void enviarNotificacionEnTiempoReal(Long usuarioId, Notificacion notificacion);
}
