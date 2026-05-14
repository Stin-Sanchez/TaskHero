package com.stinjoss.chat.websocket.chat_websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.RegistroRequest;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Flujo de registro: Debe crear el usuario en la BD y devolver 201")
    void registroExitoso() throws Exception {
        RegistroRequest request = RegistroRequest.builder()
                .nombre("Josue Test")
                .email("josue@test.com")
                .password("123456")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Josue Test"))
                .andExpect(jsonPath("$.email").value("josue@test.com"));

        assertTrue(usuarioRepository.findByEmail("josue@test.com").isPresent());
    }

    @Test
    @DisplayName("No debe permitir registrar el mismo email dos veces")
    void registroDuplicadoFalla() throws Exception {
        RegistroRequest request = RegistroRequest.builder()
                .nombre("Test")
                .email("duplicado@test.com")
                .password("123456")
                .build();

        // Primer registro
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Segundo registro con el mismo email
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
