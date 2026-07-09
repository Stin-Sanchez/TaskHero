package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.adapter;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.UsuarioLogro;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.CodigoLogro;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioLogroRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper.UsuarioLogroMapper;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaUsuarioLogroRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UsuarioLogroPersistenceAdapter implements UsuarioLogroRepository {

    private final JpaUsuarioLogroRepository jpaRepository;
    private final JpaUsuarioRepository usuarioRepository;
    private final UsuarioLogroMapper mapper;

    @Override
    public List<UsuarioLogro> findByUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByUsuarioIdAndCodigo(Long usuarioId, CodigoLogro codigo) {
        var infraCodigo = com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.enums.CodigoLogro.valueOf(codigo.name());
        return jpaRepository.existsByUsuarioIdAndCodigo(usuarioId, infraCodigo);
    }

    @Override
    public UsuarioLogro save(UsuarioLogro usuarioLogro) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioLogro.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        var entity = mapper.toEntity(usuarioLogro, usuario);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
