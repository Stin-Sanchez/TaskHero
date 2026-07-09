package com.stinjoss.chat.websocket.chat_websocket.domain.exception;

/**
 * Se lanza cuando un usuario intenta leer o escribir en un gremio del que no
 * es miembro. Nombrada explícitamente "Grupo..." (en vez de solo
 * AccessDeniedException) para no colisionar con
 * {@code org.springframework.security.access.AccessDeniedException}, que
 * Spring Security también puede lanzar y que el IDE ofrece autocompletar.
 */
public class GrupoAccessDeniedException extends DomainException {
    public GrupoAccessDeniedException(String message) {
        super(message);
    }
}
