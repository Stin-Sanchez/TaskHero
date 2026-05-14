package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioGrupoId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "grupo_id")
    private Long grupoId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioGrupoId that = (UsuarioGrupoId) o;
        return Objects.equals(usuarioId, that.usuarioId) && Objects.equals(grupoId, that.grupoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, grupoId);
    }
}