package com.stinjoss.chat.websocket.chat_websocket.application.port.in;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.GrupoChat;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Mensaje;
import java.util.List;

public interface ChatUseCase {
    Mensaje enviarMensajePrivado(Long remitenteId, Long receptorId, String contenido);
    Mensaje enviarMensajeGrupo(Long remitenteId, Long grupoId, String contenido);
    List<Mensaje> obtenerHistorialPrivado(Long user1, Long user2);
    List<Mensaje> obtenerHistorialGrupo(Long grupoId);
    GrupoChat crearGrupo(String nombre, Long creadorId, List<Long> miembrosIds);
    List<GrupoChat> listarGruposPorUsuario(Long usuarioId);
}
