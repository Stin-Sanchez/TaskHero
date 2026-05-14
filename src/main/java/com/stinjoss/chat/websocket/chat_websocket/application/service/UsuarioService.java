package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.RegistroRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.RegistrarUsuarioUseCase;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.DomainException;
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
public class UsuarioService implements RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort emailPort;

    @Override
    @Transactional
    public Usuario registrar(RegistroRequest request) {
        // 1. Validar si el email ya existe
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DomainException("El email ya está registrado");
        }

        // 2. Crear instancia de dominio
        Usuario nuevoUsuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .xpTotal(0)
                .nivelActual(1)
                .rachaDias(0)
                .isPremium(false)
                .ultimoLogin(LocalDateTime.now())
                .amigosIds(new ArrayList<>())
                .build();

        // 3. Guardar a través del puerto de dominio
        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        // 4. Enviar email de bienvenida
        emailPort.enviarCorreoBienvenida(guardado.getEmail(), guardado.getNombre());

        return guardado;
    }
}
