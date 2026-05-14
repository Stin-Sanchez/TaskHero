package com.stinjoss.chat.websocket.chat_websocket.application.port.out;

public interface EmailPort {
    void enviarCorreoBienvenida(String destinatario, String nombreUsuario);
    void enviarCorreoLogro(String destinatario, String mensaje);
}
