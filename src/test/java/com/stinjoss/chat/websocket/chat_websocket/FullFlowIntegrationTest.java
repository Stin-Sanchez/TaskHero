package com.stinjoss.chat.websocket.chat_websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.LoginRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.RegistroRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.TareaRequest;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.Prioridad;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Flujo completo: Registro -> Login -> Crear Tarea")
    void flujoCompletoRegistroLoginTarea() throws Exception {
        // 1. REGISTRO
        String nombre = "Nuevo Heroe";
        String email = "heroe.nuevo@test.com";
        String password = "password123";

        RegistroRequest registro = RegistroRequest.builder()
                .nombre(nombre)
                .email(email)
                .password(password)
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registro)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andExpect(jsonPath("$.email").value(email));

        // 2. LOGIN
        LoginRequest login = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult resultLogin = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();

        String responseBody = resultLogin.getResponse().getContentAsString();
        Map<String, Object> authResponse = objectMapper.readValue(responseBody, Map.class);
        String token = "Bearer " + authResponse.get("token");

        // 3. CREAR TAREA
        TareaRequest tareaReq = TareaRequest.builder()
                .titulo("Mi primera mision")
                .prioridad(Prioridad.BAJA)
                .build();

        mockMvc.perform(post("/api/tareas")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tareaReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titulo").value("Mi primera mision"))
                .andExpect(jsonPath("$.prioridad").value("BAJA"));

        // Verificación adicional en BD
        assertEquals(0, usuarioRepository.findByEmail(email).get().getXpTotal(), "El usuario debería tener 0 XP (el bono diario no aplica el mismo día del registro)");
    }
}
