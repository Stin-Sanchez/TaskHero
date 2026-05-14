package com.stinjoss.chat.websocket.chat_websocket.infrastructure.notification;

import com.stinjoss.chat.websocket.chat_websocket.application.port.out.ChatPushPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Mensaje;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketChatAdapter implements ChatPushPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void enviarMensajePrivado(Mensaje mensaje) {
        // Enviar al receptor
        messagingTemplate.convertAndSend("/topic/chat/private/" + mensaje.getReceptorId(), mensaje);
        // Enviar al remitente (para sincronizar otros dispositivos del mismo usuario)
        messagingTemplate.convertAndSend("/topic/chat/private/" + mensaje.getRemitenteId(), mensaje);
    }

    @Override
    public void enviarMensajeGrupo(Mensaje mensaje) {
        // Enviar al grupo: /topic/chat/group/{grupoId}
        messagingTemplate.convertAndSend("/topic/chat/group/" + mensaje.getGrupoId(), mensaje);
    }

    @Override
    public void enviarEstadoEscribiendo(Long remitenteId, String remitenteNombre, Long receptorId, Long grupoId, boolean escribiendo) {
        Object payload = new java.util.HashMap<String, Object>() {{
            put("remitenteId", remitenteId);
            put("remitenteNombre", remitenteNombre);
            put("escribiendo", escribiendo);
            if (grupoId != null) put("grupoId", grupoId);
        }};

        if (grupoId != null) {
            messagingTemplate.convertAndSend("/topic/chat/group/" + grupoId + "/typing", payload);
        } else if (receptorId != null) {
            messagingTemplate.convertAndSend("/topic/chat/private/" + receptorId + "/typing", payload);
        }
    }
}
