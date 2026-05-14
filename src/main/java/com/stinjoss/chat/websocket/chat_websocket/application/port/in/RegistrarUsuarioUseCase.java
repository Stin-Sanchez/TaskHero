package com.stinjoss.chat.websocket.chat_websocket.application.port.in;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.RegistroRequest;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;

public interface RegistrarUsuarioUseCase {
    Usuario registrar(RegistroRequest request);
}
