package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Nivel;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.NivelEntity;
import org.springframework.stereotype.Component;

@Component
public class NivelMapper {

    public Nivel toDomain(NivelEntity entity) {
        if (entity == null) return null;
        return Nivel.builder()
                .numero(entity.getNumeroNivel())
                .xpRequerido(entity.getXpRequerido())
                .funcionDesbloqueada(entity.getFuncionDesbloqueada())
                .descripcion(entity.getDescripcion())
                .build();
    }

    public NivelEntity toEntity(Nivel domain) {
        if (domain == null) return null;
        return NivelEntity.builder()
                .numeroNivel(domain.getNumero())
                .xpRequerido(domain.getXpRequerido())
                .funcionDesbloqueada(domain.getFuncionDesbloqueada())
                .descripcion(domain.getDescripcion())
                .build();
    }
}
