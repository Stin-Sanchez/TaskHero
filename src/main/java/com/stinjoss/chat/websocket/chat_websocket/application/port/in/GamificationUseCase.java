package com.stinjoss.chat.websocket.chat_websocket.application.port.in;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;

public interface GamificationUseCase {
    void procesarXpPorTareaCompletada(Long usuarioId);
    void procesarLoginDiario(Long usuarioId);
}
