package com.stinjoss.chat.websocket.chat_websocket;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.PerfilRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.JwtPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.GrupoChat;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.ChatRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SocialIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ChatRepository chatRepository;
    @Autowired private JwtPort jwtPort;
    @Autowired private ObjectMapper objectMapper;

    private String token;
    private Usuario me;
    private Usuario other;

    @Autowired private com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaUsuarioRepository jpaUsuarioRepository;
    @Autowired private com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaGrupoChatRepository jpaGrupoChatRepository;
    @Autowired private com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaUsuarioGrupoRepository jpaUsuarioGrupoRepository;
    @Autowired private com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaNotificacionRepository jpaNotificacionRepository;
    @Autowired private com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaMensajeRepository jpaMensajeRepository;

    @BeforeEach
    void setUp() {
        jpaNotificacionRepository.deleteAll();
        jpaMensajeRepository.deleteAll();
        jpaUsuarioGrupoRepository.deleteAll();
        jpaGrupoChatRepository.deleteAll();
        jpaUsuarioRepository.deleteAll();

        me = Usuario.builder()
                .nombre("MiHeroe")
                .email("me@test.com")
                .passwordHash("hash")
                .nivelActual(5)
                .build();
        me = usuarioRepository.save(me);

        other = Usuario.builder()
                .nombre("OtroHeroe")
                .email("other@test.com")
                .passwordHash("hash")
                .nivelActual(1)
                .build();
        other = usuarioRepository.save(other);

        token = jwtPort.generarToken(me);
    }

    @Test
    @DisplayName("Debe actualizar el avatar del usuario")
    void actualizarAvatarExito() throws Exception {
        PerfilRequest request = new PerfilRequest();
        request.setNombre("NuevoNombre");
        request.setAvatarUrl("http://avatar.com/img.png");

        mockMvc.perform(put("/api/auth/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("http://avatar.com/img.png"));
    }

    @Test
    @DisplayName("Flujo Social: Crear gremio e invitar miembro")
    void flujoGremioInvitacion() throws Exception {
        // 1. Crear Gremio
        GrupoChat guild = GrupoChat.builder()
                .nombre("Los Invencibles")
                .miembrosIds(new ArrayList<>())
                .build();
        guild = chatRepository.saveGrupo(guild);
        chatRepository.agregarMiembroAlGrupo(guild.getId(), me.getId());

        // 2. Invitar al otro héroe
        mockMvc.perform(post("/api/chat/grupos/" + guild.getId() + "/invitar/" + other.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Aceptar invitación (como el otro usuario)
        String otherToken = jwtPort.generarToken(other);
        mockMvc.perform(post("/api/chat/grupos/" + guild.getId() + "/aceptar")
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk());
    }
}
