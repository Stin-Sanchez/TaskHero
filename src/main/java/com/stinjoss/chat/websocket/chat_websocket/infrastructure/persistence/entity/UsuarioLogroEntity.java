package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.enums.CodigoLogro;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_logros", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "codigo"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioLogroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CodigoLogro codigo;

    @CreationTimestamp
    @Column(name = "fecha_obtenido", nullable = false, updatable = false)
    private LocalDateTime fechaObtenido;
}
