package com.stinjoss.chat.websocket.chat_websocket.domain.repository;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Mensaje;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.GrupoChat;
import java.util.List;

public interface ChatRepository {
    Mensaje saveMensaje(Mensaje mensaje);
    List<Mensaje> findHistorialPrivado(Long user1, Long user2);
    List<Mensaje> findHistorialGrupo(Long grupoId);
    GrupoChat saveGrupo(GrupoChat grupo);
    List<GrupoChat> findGruposByUsuarioId(Long usuarioId);
}
