package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.adapter;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.TareaRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.TareaEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper.TareaMapper;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaTareaRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TareaPersistenceAdapter implements TareaRepository {

    private final JpaTareaRepository jpaRepository;
    private final JpaUsuarioRepository usuarioRepository;
    private final TareaMapper mapper;

    @Override
    public Optional<Tarea> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Tarea> findByUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Tarea save(Tarea tarea) {
        UsuarioEntity usuario = usuarioRepository.findById(tarea.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para la tarea"));
        
        TareaEntity entity = mapper.toEntity(tarea, usuario);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
