package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.enums.RolGrupo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario_grupo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioGrupoEntity {

    @EmbeddedId
    private UsuarioGrupoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("grupoId")
    @JoinColumn(name = "grupo_id")
    private GrupoChatEntity grupo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 50)
    private RolGrupo rol = RolGrupo.MIEMBRO;
}