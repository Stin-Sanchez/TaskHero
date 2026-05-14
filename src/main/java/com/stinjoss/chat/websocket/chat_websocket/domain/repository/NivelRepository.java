package com.stinjoss.chat.websocket.chat_websocket.domain.repository;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Nivel;
import java.util.List;

public interface NivelRepository {
    List<Nivel> findAll();
}
