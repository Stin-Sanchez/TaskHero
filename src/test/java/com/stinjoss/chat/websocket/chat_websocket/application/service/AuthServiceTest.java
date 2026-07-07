package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GamificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.JwtPort;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.PerfilRequest;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.DomainException;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtPort jwtPort;
    @Mock private GamificationUseCase gamificationUseCase;
    @Mock private EmailPort emailPort;

    @InjectMocks private AuthService authService;
    @InjectMocks private UsuarioService usuarioService;

    @Test
    @DisplayName("Debe generar un token de recuperación y enviar email")
    void solicitarRecuperacionExito() {
        String email = "test@test.com";
        Usuario usuario = Usuario.builder().email(email).build();
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        authService.solicitarRecuperacion(email);

        assertNotNull(usuario.getResetPasswordToken());
        assertNotNull(usuario.getTokenExpiration());
        verify(usuarioRepository).save(usuario);
        verify(emailPort).enviarCorreoRecuperacion(eq(email), anyString());
    }

    @Test
    @DisplayName("Debe actualizar nombre del perfil")
    void actualizarPerfilNombreExito() {
        Long userId = 1L;
        Usuario usuario = Usuario.builder().id(userId).nombre("Viejo").build();
        PerfilRequest request = new PerfilRequest();
        request.setNombre("Nuevo");

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenReturn(usuario);

        Usuario resultado = usuarioService.actualizarPerfil(userId, request);

        assertEquals("Nuevo", resultado.getNombre());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Debe fallar al cambiar contraseña si la actual es incorrecta")
    void cambiarPasswordError() {
        Long userId = 1L;
        Usuario usuario = Usuario.builder().id(userId).passwordHash("hash").build();
        PerfilRequest request = new PerfilRequest();
        request.setNombre("Nuevo");
        request.setPasswordActual("wrong");
        request.setPasswordNueva("nueva123");

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(DomainException.class, () -> usuarioService.actualizarPerfil(userId, request));
    }
}
