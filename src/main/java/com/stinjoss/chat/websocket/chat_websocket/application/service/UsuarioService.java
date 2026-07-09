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

/**
 * Casos de uso de registro de cuenta (RF-01) y edición de perfil (RF-04).
 */
@Service
@RequiredArgsConstructor
public class UsuarioService implements RegistrarUsuarioUseCase, GestionarPerfilUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort emailPort;

    /**
     * Registra un nuevo héroe con sus valores iniciales de gamificación en cero
     * (Nivel 1, sin XP ni racha) y envía el correo de bienvenida.
     * <p>
     * {@code ultimoLogin} se fija al momento del registro de forma intencional:
     * así el primer inicio de sesión del usuario no cuenta como "primer login
     * del día" y no duplica el bono de racha que ya implícitamente arrancó al crear la cuenta.
     *
     * @throws DomainException si el email ya está registrado.
     */
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

    /**
     * Actualiza nombre y avatar del héroe, y opcionalmente su contraseña.
     * <p>
     * El cambio de contraseña es un sub-flujo condicional: solo se valida y
     * aplica si el usuario envía {@code passwordNueva}; en ese caso es
     * obligatorio confirmar la contraseña actual, como medida contra un
     * secuestro de sesión que intente cambiar la contraseña sin conocerla.
     *
     * @throws ResourceNotFoundException si el usuario no existe.
     * @throws DomainException si se intenta cambiar la contraseña sin indicar correctamente la actual.
     */
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
