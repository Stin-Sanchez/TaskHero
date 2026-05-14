package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.adapter;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Notificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.NotificacionRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.NotificacionEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper.NotificacionMapper;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaNotificacionRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificacionPersistenceAdapter implements NotificacionRepository {

    private final JpaNotificacionRepository jpaRepository;
    private final JpaUsuarioRepository usuarioRepository;
    private final NotificacionMapper mapper;

    @Override
    public Notificacion save(Notificacion notificacion) {
        UsuarioEntity usuario = usuarioRepository.findById(notificacion.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        NotificacionEntity entity = mapper.toEntity(notificacion, usuario);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Notificacion> findByUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long notificacionId) {
        jpaRepository.findById(notificacionId).ifPresent(n -> {
            n.setLeida(true);
            jpaRepository.save(n);
        });
    }
}
