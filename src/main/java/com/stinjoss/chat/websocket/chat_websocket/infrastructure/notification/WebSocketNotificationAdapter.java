package com.stinjoss.chat.websocket.chat_websocket.infrastructure.notification;

import com.stinjoss.chat.websocket.chat_websocket.application.port.out.NotificacionPushPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Notificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketNotificationAdapter implements NotificacionPushPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void enviarNotificacionEnTiempoReal(Long usuarioId, Notificacion notificacion) {
        // Enviar al canal privado del usuario: /user/{email}/queue/notifications
        // Usaremos el ID o Email. Como el frontend se suscribe a su propio canal:
        // En STOMP se suele usar convertAndSendToUser
        
        String destination = "/queue/notifications";
        // Nota: El usuario debe estar autenticado en el socket para recibir mensajes vía 'ToUser'
        // Por ahora lo enviamos a una ruta predecible basada en el ID para simplificar el desarrollo vanilla
        messagingTemplate.convertAndSend("/topic/notifications/" + usuarioId, notificacion);
    }
}
