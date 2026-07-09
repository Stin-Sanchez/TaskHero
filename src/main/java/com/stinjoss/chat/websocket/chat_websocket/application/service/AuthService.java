package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.AuthResponse;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.LoginRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.AutenticarUsuarioUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GamificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.JwtPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de autenticación y recuperación de contraseña (RF-02/RF-03).
 * <p>
 * Un login exitoso no solo emite el token JWT: también dispara el
 * procesamiento de racha diaria y XP del héroe (RF-11), delegado en
 * {@link GamificationUseCase#procesarLoginDiario}.
 */
@Service
@RequiredArgsConstructor
public class AuthService implements AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtPort jwtPort;
    private final GamificationUseCase gamificationUseCase;
    private final com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort emailPort;

    /**
     * Autentica credenciales y, si son válidas, emite un JWT y procesa el
     * login diario del héroe (racha + XP).
     * <p>
     * Se lanza el mismo mensaje genérico ("Credenciales inválidas") tanto si
     * el email no existe como si la contraseña no coincide, para no revelar a
     * un atacante si un correo está o no registrado en el sistema.
     *
     * @throws com.stinjoss.chat.websocket.chat_websocket.domain.exception.InvalidCredentialsException
     *         si el email no existe o la contraseña no coincide (HTTP 401).
     */
    @Override
    public AuthResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.stinjoss.chat.websocket.chat_websocket.domain.exception.InvalidCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new com.stinjoss.chat.websocket.chat_websocket.domain.exception.InvalidCredentialsException("Credenciales inválidas");
        }

        // Disparar lógica de login diario (XP + Racha)
        gamificationUseCase.procesarLoginDiario(usuario.getId());

        String token = jwtPort.generarToken(usuario);

        return AuthResponse.builder()
                .token(token)
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .build();
    }

    /**
     * Genera un código de recuperación de un solo uso, válido por 15 minutos
     * (RF-03), y lo envía por correo electrónico.
     *
     * @throws com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException
     *         si no existe un usuario con ese correo.
     */
    @Override
    public void solicitarRecuperacion(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException("Usuario no encontrado"));

        String token = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        usuario.setResetPasswordToken(token);
        usuario.setTokenExpiration(java.time.LocalDateTime.now().plusMinutes(15));

        usuarioRepository.save(usuario);
        emailPort.enviarCorreoRecuperacion(email, token);
    }

    /**
     * Completa el flujo de recuperación validando el código recibido por
     * correo antes de aplicar la nueva contraseña.
     *
     * @throws com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException si el usuario no existe.
     * @throws com.stinjoss.chat.websocket.chat_websocket.domain.exception.DomainException si el código no coincide o ya expiró (más de 15 minutos).
     */
    @Override
    public void resetearPassword(String email, String token, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException("Usuario no encontrado"));

        if (usuario.getResetPasswordToken() == null || !usuario.getResetPasswordToken().equals(token)) {
            throw new com.stinjoss.chat.websocket.chat_websocket.domain.exception.DomainException("Token inválido");
        }

        if (usuario.getTokenExpiration().isBefore(java.time.LocalDateTime.now())) {
            throw new com.stinjoss.chat.websocket.chat_websocket.domain.exception.DomainException("El token ha expirado");
        }

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setResetPasswordToken(null);
        usuario.setTokenExpiration(null);

        usuarioRepository.save(usuario);
    }
}
