package com.stinjoss.chat.websocket.chat_websocket.domain.repository;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    Optional<Usuario> findById(Long id);
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findAll();
    Usuario save(Usuario usuario);
}
