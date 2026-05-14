package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioGrupoEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioGrupoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaUsuarioGrupoRepository extends JpaRepository<UsuarioGrupoEntity, UsuarioGrupoId> {
    
    @Query("SELECT ug.grupo.id FROM UsuarioGrupoEntity ug WHERE ug.usuario.id = :usuarioId")
    List<Long> findGrupoIdsByUsuarioId(@Param("usuarioId") Long usuarioId);

    List<UsuarioGrupoEntity> findByGrupoId(Long grupoId);
}
