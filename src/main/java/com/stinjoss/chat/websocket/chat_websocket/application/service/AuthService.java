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

@Service
@RequiredArgsConstructor
public class AuthService implements AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtPort jwtPort;
    private final GamificationUseCase gamificationUseCase;
    private final com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort emailPort;

    @Override
    public AuthResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
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
