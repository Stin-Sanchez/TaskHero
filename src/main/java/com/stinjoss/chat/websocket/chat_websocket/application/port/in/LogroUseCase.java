package com.stinjoss.chat.websocket.chat_websocket.application.port.in;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.LogroResponse;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;

import java.util.List;

public interface LogroUseCase {
    /**
     * Recibe el {@link Usuario} ya cargado (en vez de solo su ID) para que el
     * llamador reutilice la instancia que ya tenía en memoria y no se dispare
     * una segunda consulta a la base de datos por el mismo usuario.
     */
    void evaluarLogros(Usuario usuario);
    List<LogroResponse> listarLogros(Long usuarioId);
}
