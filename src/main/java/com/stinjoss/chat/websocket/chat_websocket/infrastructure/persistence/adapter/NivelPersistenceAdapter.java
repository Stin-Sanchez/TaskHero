package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.adapter;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Nivel;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.NivelRepository;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper.NivelMapper;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaNivelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NivelPersistenceAdapter implements NivelRepository {

    private final JpaNivelRepository jpaRepository;
    private final NivelMapper mapper;

    @Override
    public List<Nivel> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
