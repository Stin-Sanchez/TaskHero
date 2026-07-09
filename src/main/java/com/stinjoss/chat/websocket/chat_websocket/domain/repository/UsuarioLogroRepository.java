package com.stinjoss.chat.websocket.chat_websocket.domain.repository;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.UsuarioLogro;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.CodigoLogro;

import java.util.List;

public interface UsuarioLogroRepository {
    List<UsuarioLogro> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioIdAndCodigo(Long usuarioId, CodigoLogro codigo);
    UsuarioLogro save(UsuarioLogro usuarioLogro);
}
