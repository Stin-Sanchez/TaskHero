package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Builder.Default
    @Column(name = "xp_total", nullable = false, columnDefinition = "integer default 0")
    private Integer xpTotal = 0;

    @Builder.Default
    @Column(name = "nivel_actual", nullable = false, columnDefinition = "integer default 1")
    private Integer nivelActual = 1;

    @Builder.Default
    @Column(name = "racha_dias", nullable = false, columnDefinition = "integer default 0")
    private Integer rachaDias = 0;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "is_premium", nullable = false, columnDefinition = "boolean default false")
    private Boolean isPremium = false;
}