package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.adapter;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.GrupoChat;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Mensaje;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.ChatRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.GrupoChatEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.MensajeEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper.ChatMapper;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioGrupoEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioGrupoId;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaGrupoChatRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaMensajeRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaUsuarioGrupoRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatPersistenceAdapter implements ChatRepository {

    private final JpaMensajeRepository mensajeRepository;
    private final JpaGrupoChatRepository grupoChatRepository;
    private final JpaUsuarioRepository usuarioRepository;
    private final JpaUsuarioGrupoRepository usuarioGrupoRepository;
    private final ChatMapper mapper;

    @Override
    public Mensaje saveMensaje(Mensaje mensaje) {
        // ... (existing code same as before, but I'll provide the whole block)
        UsuarioEntity remitente = usuarioRepository.findById(mensaje.getRemitenteId())
                .orElseThrow(() -> new RuntimeException("Remitente no encontrado"));

        UsuarioEntity receptor = null;
        if (mensaje.getReceptorId() != null) {
            receptor = usuarioRepository.findById(mensaje.getReceptorId())
                    .orElseThrow(() -> new RuntimeException("Receptor no encontrado"));
        }

        GrupoChatEntity grupo = null;
        if (mensaje.getGrupoId() != null) {
            grupo = grupoChatRepository.findById(mensaje.getGrupoId())
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
        }

        MensajeEntity entity = MensajeEntity.builder()
                .id(mensaje.getId())
                .remitente(remitente)
                .receptor(receptor)
                .grupo(grupo)
                .contenido(mensaje.getContenido())
                .build();

        return mapper.toDomain(mensajeRepository.save(entity));
    }

    @Override
    public List<Mensaje> findHistorialPrivado(Long user1, Long user2) {
        return mensajeRepository.findHistorialPrivado(user1, user2).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Mensaje> findHistorialGrupo(Long grupoId) {
        return mensajeRepository.findByGrupoIdOrderByTimestampAsc(grupoId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public GrupoChat saveGrupo(GrupoChat grupo) {
        GrupoChatEntity entity = GrupoChatEntity.builder()
                .id(grupo.getId())
                .nombre(grupo.getNombre())
                .build();
        
        GrupoChatEntity saved = grupoChatRepository.save(entity);

        if (grupo.getMiembrosIds() != null) {
            grupo.getMiembrosIds().forEach(uid -> {
                UsuarioEntity user = usuarioRepository.findById(uid).orElseThrow();
                UsuarioGrupoEntity ug = UsuarioGrupoEntity.builder()
                        .id(new UsuarioGrupoId(uid, saved.getId()))
                        .usuario(user)
                        .grupo(saved)
                        .build();
                usuarioGrupoRepository.save(ug);
            });
        }

        return mapper.toDomain(saved);
    }

    @Override
    public List<GrupoChat> findGruposByUsuarioId(Long usuarioId) {
        List<Long> grupoIds = usuarioGrupoRepository.findGrupoIdsByUsuarioId(usuarioId);
        return grupoChatRepository.findAllById(grupoIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
