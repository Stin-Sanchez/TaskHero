package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Mensaje;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.GrupoChat;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.MensajeEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.GrupoChatEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ChatMapper {

    public Mensaje toDomain(MensajeEntity entity) {
        if (entity == null) return null;
        return Mensaje.builder()
                .id(entity.getId())
                .remitenteId(entity.getRemitente().getId())
                .remitenteNombre(entity.getRemitente().getNombre())
                .receptorId(entity.getReceptor() != null ? entity.getReceptor().getId() : null)
                .grupoId(entity.getGrupo() != null ? entity.getGrupo().getId() : null)
                .contenido(entity.getContenido())
                .timestamp(entity.getTimestamp())
                .build();
    }

    public GrupoChat toDomain(GrupoChatEntity entity) {
        if (entity == null) return null;
        return GrupoChat.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .miembrosIds(new ArrayList<>()) // Se cargaría vía relación si fuera necesario
                .build();
    }
}
