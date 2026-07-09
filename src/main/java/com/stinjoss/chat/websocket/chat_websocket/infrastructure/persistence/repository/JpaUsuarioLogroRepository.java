package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioLogroEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.enums.CodigoLogro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaUsuarioLogroRepository extends JpaRepository<UsuarioLogroEntity, Long> {
    List<UsuarioLogroEntity> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioIdAndCodigo(Long usuarioId, CodigoLogro codigo);
}
