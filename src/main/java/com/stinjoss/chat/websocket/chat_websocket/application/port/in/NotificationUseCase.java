package com.stinjoss.chat.websocket.chat_websocket.application.port.in;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;

public interface NotificationUseCase {
    void enviarNotificacion(Long usuarioId, TipoNotificacion tipo, String mensaje);
}
