package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.enums.PrioridadTarea;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tareas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TareaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private PrioridadTarea prioridad = PrioridadTarea.MEDIA;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean completada = false;

    @Column(length = 50)
    private String categoria;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}