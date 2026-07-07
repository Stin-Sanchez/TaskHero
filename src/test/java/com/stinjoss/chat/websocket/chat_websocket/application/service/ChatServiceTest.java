package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.port.in.NotificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.ChatPushPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.GrupoChat;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.ChatRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatRepository chatRepository;
    @Mock private ChatPushPort chatPushPort;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificationUseCase notificationUseCase;

    @InjectMocks private ChatService chatService;

    @Test
    @DisplayName("Debe enviar notificación de invitación al gremio")
    void invitarMiembroExito() {
        Long senderId = 1L;
        Long receiverId = 2L;
        Long guildId = 10L;

        Usuario sender = Usuario.builder().id(senderId).nombre("Sender").nivelActual(5).build();
        GrupoChat guild = GrupoChat.builder().id(guildId).nombre("Gremio Test").build();

        when(usuarioRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(chatRepository.findGrupoById(guildId)).thenReturn(Optional.of(guild));

        chatService.invitarMiembro(senderId, receiverId, guildId);

        verify(notificationUseCase).enviarNotificacion(eq(receiverId), any(), contains("Gremio Test"));
    }

    @Test
    @DisplayName("Debe agregar miembro al gremio al aceptar invitación")
    void aceptarInvitacionExito() {
        Long userId = 1L;
        Long guildId = 10L;
        GrupoChat guild = GrupoChat.builder().id(guildId).nombre("Gremio Test").build();

        when(chatRepository.findGrupoById(guildId)).thenReturn(Optional.of(guild));

        chatService.aceptarInvitacion(userId, guildId);

        verify(chatRepository).agregarMiembroAlGrupo(guildId, userId);
        verify(notificationUseCase).enviarNotificacion(eq(userId), any(), anyString());
    }

    @Test
    @DisplayName("Debe fallar al invitar si el grupo no existe")
    void invitarMiembroGrupoNoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(Usuario.builder().nivelActual(5).build()));
        when(chatRepository.findGrupoById(10L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class, 
            () -> chatService.invitarMiembro(1L, 2L, 10L));
    }
}
