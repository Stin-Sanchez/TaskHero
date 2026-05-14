package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.converter.EncryptionConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remitente_id", nullable = false)
    private UsuarioEntity remitente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private GrupoChatEntity grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptor_id")
    private UsuarioEntity receptor;

    @Convert(converter = EncryptionConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}