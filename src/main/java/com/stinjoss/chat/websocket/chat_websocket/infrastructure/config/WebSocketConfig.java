package com.stinjoss.chat.websocket.chat_websocket.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint que usará el frontend para conectar (SockJS)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefijo para los mensajes que el servidor envía a los clientes
        // /topic para broadcasts (notificaciones generales)
        // /queue para mensajes privados (chat)
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefijo para los mensajes que los clientes envían al servidor
        registry.setApplicationDestinationPrefixes("/app");

        // Prefijo para mensajes privados usuario a usuario
        registry.setUserDestinationPrefix("/user");
    }
}
