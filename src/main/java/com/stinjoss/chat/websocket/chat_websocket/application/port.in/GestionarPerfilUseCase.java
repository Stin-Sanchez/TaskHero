package com.stinjoss.chat.websocket.chat_websocket.application.port.in;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.PerfilRequest;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;

public interface GestionarPerfilUseCase {
    Usuario actualizarPerfil(Long usuarioId, PerfilRequest request);
}
