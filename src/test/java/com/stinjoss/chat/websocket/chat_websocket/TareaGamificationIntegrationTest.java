package com.stinjoss.chat.websocket.chat_websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.LoginRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.RegistroRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.TareaRequest;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.Prioridad;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TareaGamificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String token;
    private Long usuarioId;

    @BeforeEach
    void setup() throws Exception {
        // 1. Registrar
        RegistroRequest registro = RegistroRequest.builder()
                .nombre("Josue Gamer")
                .email("josue.gamer@test.com")
                .password("password123")
                .build();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registro)));

        // 2. Login para obtener token
        LoginRequest login = LoginRequest.builder()
                .email("josue.gamer@test.com")
                .password("password123")
                .build();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
        this.token = "Bearer " + map.get("token");
        
        this.usuarioId = usuarioRepository.findByEmail("josue.gamer@test.com").get().getId();
    }

    @Test
    @DisplayName("Al completar una tarea, el usuario debe ganar XP")
    void ganarXpAlCompletarTarea() throws Exception {
        // 1. Crear Tarea
        TareaRequest tareaReq = TareaRequest.builder()
                .titulo("Tarea de prueba")
                .prioridad(Prioridad.ALTA)
                .build();

        MvcResult resultTarea = mockMvc.perform(post("/api/tareas")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tareaReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> tareaMap = objectMapper.readValue(resultTarea.getResponse().getContentAsString(), Map.class);
        Integer tareaId = (Integer) tareaMap.get("id");

        // 2. Completar Tarea
        mockMvc.perform(patch("/api/tareas/" + tareaId + "/completar")
                .header("Authorization", token))
                .andExpect(status().isOk());

        // 3. Verificar XP del usuario (XP_LOGIN_DIARIO(5) + XP_TAREA_COMPLETADA(10) = 15)
        // Nota: El login diario se dispara en el AuthService.autenticar
        Usuario usuarioActualizado = usuarioRepository.findById(usuarioId).get();
        assertEquals(15, usuarioActualizado.getXpTotal());
    }
}
