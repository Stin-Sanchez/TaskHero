package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.NivelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNivelRepository extends JpaRepository<NivelEntity, Integer> {
}
