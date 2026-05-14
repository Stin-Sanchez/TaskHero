package com.stinjoss.chat.websocket.chat_websocket.application.port.out;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;

public interface JwtPort {
    String generarToken(Usuario usuario);
    String getEmailFromToken(String token);
    boolean validarToken(String token);
}
