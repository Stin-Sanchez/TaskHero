package com.stinjoss.chat.websocket.chat_websocket.application.port.out;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Mensaje;

public interface ChatPushPort {
    void enviarMensajePrivado(Mensaje mensaje);
    void enviarMensajeGrupo(Mensaje mensaje);
    void enviarEstadoEscribiendo(Long remitenteId, String remitenteNombre, Long receptorId, Long grupoId, boolean escribiendo);
}
