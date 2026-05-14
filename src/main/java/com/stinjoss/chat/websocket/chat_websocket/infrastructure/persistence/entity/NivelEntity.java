package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "niveles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NivelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_nivel", nullable = false, unique = true)
    private Integer numeroNivel;

    @Column(name = "xp_requerido", nullable = false)
    private Integer xpRequerido;

    @Column(name = "funcion_desbloqueada", length = 200)
    private String funcionDesbloqueada;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}