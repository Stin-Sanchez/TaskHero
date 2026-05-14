package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class UsuarioMapper {

    public Usuario toDomain(UsuarioEntity entity) {
        if (entity == null) return null;

        return Usuario.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .xpTotal(entity.getXpTotal())
                .nivelActual(entity.getNivelActual())
                .rachaDias(entity.getRachaDias())
                .isPremium(entity.getIsPremium())
                .ultimoLogin(entity.getUltimoLogin())
                .amigosIds(new ArrayList<>()) // Se cargaría en un servicio aparte si es necesario
                .build();
    }

    public UsuarioEntity toEntity(Usuario domain) {
        if (domain == null) return null;

        return UsuarioEntity.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .xpTotal(domain.getXpTotal())
                .nivelActual(domain.getNivelActual())
                .rachaDias(domain.getRachaDias())
                .isPremium(domain.isPremium())
                .ultimoLogin(domain.getUltimoLogin())
                .build();
    }
}
