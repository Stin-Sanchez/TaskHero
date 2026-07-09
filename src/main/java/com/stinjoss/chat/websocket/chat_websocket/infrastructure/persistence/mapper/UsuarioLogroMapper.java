package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.UsuarioLogro;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.CodigoLogro;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioLogroEntity;
import org.springframework.stereotype.Component;

@Component
public class UsuarioLogroMapper {

    public UsuarioLogro toDomain(UsuarioLogroEntity entity) {
        if (entity == null) return null;
        return UsuarioLogro.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuario().getId())
                .codigo(CodigoLogro.valueOf(entity.getCodigo().name()))
                .fechaObtenido(entity.getFechaObtenido())
                .build();
    }

    public UsuarioLogroEntity toEntity(UsuarioLogro domain, UsuarioEntity usuario) {
        if (domain == null) return null;
        return UsuarioLogroEntity.builder()
                .id(domain.getId())
                .usuario(usuario)
                .codigo(com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.enums.CodigoLogro.valueOf(domain.getCodigo().name()))
                .build();
    }
}
