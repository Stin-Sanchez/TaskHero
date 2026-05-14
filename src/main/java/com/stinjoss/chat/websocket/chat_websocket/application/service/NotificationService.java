package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.port.in.NotificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.NotificacionPushPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Notificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionPushPort notificacionPushPort;

    @Override
    @Transactional
    public void enviarNotificacion(Long usuarioId, TipoNotificacion tipo, String mensaje) {
        Notificacion notificacion = Notificacion.builder()
                .usuarioId(usuarioId)
                .tipo(tipo)
                .mensaje(mensaje)
                .leida(false)
                .fechaCreacion(LocalDateTime.now())
                .build();
        
        Notificacion guardada = notificacionRepository.save(notificacion);
        notificacionPushPort.enviarNotificacionEnTiempoReal(usuarioId, guardada);
    }
}
