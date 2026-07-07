package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.RegistroRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.PerfilRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.RegistrarUsuarioUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GestionarPerfilUseCase;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.DomainException;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UsuarioService implements RegistrarUsuarioUseCase, GestionarPerfilUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort emailPort;

    @Override
    @Transactional
    public Usuario registrar(RegistroRequest request) {
        // ... (resto del código igual)
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DomainException("El email ya está registrado");
        }

        Usuario nuevoUsuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .xpTotal(0)
                .nivelActual(1)
                .rachaDias(0)
                .isPremium(false)
                .ultimoLogin(LocalDateTime.now()) // Intencional: evita el bono de login diario el mismo día del registro.
                .amigosIds(new ArrayList<>())
                .build();

        Usuario guardado = usuarioRepository.save(nuevoUsuario);
        emailPort.enviarCorreoBienvenida(guardado.getEmail(), guardado.getNombre());

        return guardado;
    }

    @Override
    @Transactional
    public Usuario actualizarPerfil(Long usuarioId, PerfilRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.setNombre(request.getNombre());
        usuario.setAvatarUrl(request.getAvatarUrl());

        // Si desea cambiar la contraseña
        if (request.getPasswordNueva() != null && !request.getPasswordNueva().isBlank()) {
            if (request.getPasswordActual() == null || !passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
                throw new DomainException("La contraseña actual es incorrecta");
            }
            usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        }

        return usuarioRepository.save(usuario);
    }
}
