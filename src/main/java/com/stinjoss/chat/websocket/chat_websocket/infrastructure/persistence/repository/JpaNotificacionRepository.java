package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaNotificacionRepository extends JpaRepository<NotificacionEntity, Long> {
    List<NotificacionEntity> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);
}
