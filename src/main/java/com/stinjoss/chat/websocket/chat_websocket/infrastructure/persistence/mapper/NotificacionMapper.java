package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Notificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.NotificacionEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificacionMapper {

    public Notificacion toDomain(NotificacionEntity entity) {
        if (entity == null) return null;
        return Notificacion.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuario().getId())
                .tipo(TipoNotificacion.valueOf(entity.getTipo().name()))
                .mensaje(entity.getMensaje())
                .leida(entity.getLeida())
                .fechaCreacion(entity.getCreatedAt())
                .build();
    }

    public NotificacionEntity toEntity(Notificacion domain, UsuarioEntity usuario) {
        if (domain == null) return null;
        return NotificacionEntity.builder()
                .id(domain.getId())
                .usuario(usuario)
                .tipo(com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.enums.TipoNotificacion.valueOf(domain.getTipo().name()))
                .mensaje(domain.getMensaje())
                .leida(domain.isLeida())
                .build();
    }
}
