package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.MensajeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaMensajeRepository extends JpaRepository<MensajeEntity, Long> {
    
    @Query("SELECT m FROM MensajeEntity m WHERE (m.remitente.id = :u1 AND m.receptor.id = :u2) OR (m.remitente.id = :u2 AND m.receptor.id = :u1) ORDER BY m.timestamp ASC")
    List<MensajeEntity> findHistorialPrivado(@Param("u1") Long user1Id, @Param("u2") Long user2Id);

    List<MensajeEntity> findByGrupoIdOrderByTimestampAsc(Long grupoId);
}
