package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.GrupoChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JpaGrupoChatRepository extends JpaRepository<GrupoChatEntity, Long> {
    List<GrupoChatEntity> findByNombreContainingIgnoreCase(String nombre);
}
