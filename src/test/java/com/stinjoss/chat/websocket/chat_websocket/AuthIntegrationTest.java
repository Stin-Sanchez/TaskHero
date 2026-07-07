package com.stinjoss.chat.websocket.chat_websocket;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.LoginRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.PasswordResetRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailPort emailPort; // Inyectado desde el Mock de la configuración de abajo

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public EmailPort emailPort() {
            return mock(EmailPort.class);
        }
    }

    @Test
    @DisplayName("Flujo completo: Solicitar recuperación y resetear contraseña")
    void flujoRecuperacionCompleto() throws Exception {
        // 1. Asegurar usuario existe
        String email = "heroe@test.com";
        if (usuarioRepository.findByEmail(email).isEmpty()) {
            Usuario u = Usuario.builder()
                    .nombre("Heroe")
                    .email(email)
                    .passwordHash("hash")
                    .build();
            usuarioRepository.save(u);
        }

        // 2. Solicitar recuperación
        mockMvc.perform(post("/api/auth/forgot-password")
                .param("email", email))
                .andExpect(status().isOk());

        verify(emailPort).enviarCorreoRecuperacion(eq(email), anyString());

        // 3. Obtener token de la DB para simular lectura del email
        Usuario usuario = usuarioRepository.findByEmail(email).get();
        String token = usuario.getResetPasswordToken();

        // 4. Resetear contraseña
        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setEmail(email);
        resetRequest.setToken(token);
        resetRequest.setNuevaPassword("NuevaPass123!");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());
    }
}
