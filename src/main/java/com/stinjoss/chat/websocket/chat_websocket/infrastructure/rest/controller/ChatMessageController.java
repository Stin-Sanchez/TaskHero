package com.stinjoss.chat.websocket.chat_websocket.infrastructure.rest.controller;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.ChatMessageRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.ChatUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.ChatPushPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final ChatUseCase chatUseCase;
    private final ChatPushPort chatPushPort;
    private final UsuarioRepository usuarioRepository;

    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Payload ChatMessageRequest request, Authentication authentication) {
        // ... (existing code)
        if (authentication == null) {
            log.error("¡ALERTA! Intento de envío de mensaje sin autenticación de socket");
            return;
        }

        String email = authentication.getName();
        
        try {
            Usuario remitente = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Remitente no encontrado"));

            chatUseCase.enviarMensajePrivado(remitente.getId(), request.getReceptorId(), request.getContenido());
            log.info("Mensaje enviado exitosamente de {} para {}", email, request.getReceptorId());
        } catch (Exception e) {
            log.error("Error al enviar mensaje privado desde {}: {}", email, e.getMessage());
        }
    }

    @MessageMapping("/chat.sendGroup")
    public void sendGroupMessage(@Payload ChatMessageRequest request, Authentication authentication) {
        if (authentication == null) return;

        String email = authentication.getName();
        try {
            Usuario remitente = usuarioRepository.findByEmail(email).orElseThrow();
            chatUseCase.enviarMensajeGrupo(remitente.getId(), request.getGrupoId(), request.getContenido());
        } catch (Exception e) {
            log.error("Error al enviar mensaje grupal: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload ChatMessageRequest request, Authentication authentication) {
        if (authentication == null) return;

        String email = authentication.getName();
        try {
            Usuario remitente = usuarioRepository.findByEmail(email).orElseThrow();
            chatPushPort.enviarEstadoEscribiendo(
                    remitente.getId(), 
                    remitente.getNombre(),
                    request.getReceptorId(), 
                    request.getGrupoId(), 
                    request.isEscribiendo()
            );
        } catch (Exception e) {
            log.error("Error al procesar estado escribiendo: {}", e.getMessage());
        }
    }
}
