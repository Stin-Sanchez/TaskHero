package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.port.in.LogroUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.NotificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Nivel;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.NivelRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.NotificacionRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.TareaRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NivelRepository nivelRepository;
    @Mock private TareaRepository tareaRepository;
    @Mock private NotificationUseCase notificationUseCase;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private EmailPort emailPort;
    @Mock private LogroUseCase logroUseCase;

    @InjectMocks private GamificationService gamificationService;

    @Test
    @DisplayName("Debe otorgar bono de día completado si no hay tareas pendientes para hoy")
    void bonoDiaCompletadoExito() {
        Long userId = 1L;
        Usuario usuario = Usuario.builder().id(userId).xpTotal(0).nivelActual(1).build();
        LocalDate hoy = LocalDate.now();
        List<Tarea> tareas = List.of(
                Tarea.builder().fechaLimite(hoy).completada(true).build()
        );

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(nivelRepository.findAll()).thenReturn(List.of());
        when(tareaRepository.findByUsuarioId(userId)).thenReturn(tareas);

        gamificationService.procesarXpPorTareaCompletada(userId);

        // 10 XP base + 50 XP bono = 60 XP
        verify(usuarioRepository).save(argThat(u -> u.getXpTotal() == 60));
        verify(notificationUseCase, times(2)).enviarNotificacion(eq(userId), any(), anyString());
    }

    @Test
    @DisplayName("No debe otorgar bono si falta alguna tarea de hoy por completar")
    void bonoDiaCompletadoFalla() {
        Long userId = 1L;
        Usuario usuario = Usuario.builder().id(userId).xpTotal(0).nivelActual(1).build();
        LocalDate hoy = LocalDate.now();
        List<Tarea> tareas = List.of(
                Tarea.builder().fechaLimite(hoy).completada(true).build(),
                Tarea.builder().fechaLimite(hoy).completada(false).build()
        );

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tareaRepository.findByUsuarioId(userId)).thenReturn(tareas);

        gamificationService.procesarXpPorTareaCompletada(userId);

        // Solo 10 XP base
        verify(usuarioRepository).save(argThat(u -> u.getXpTotal() == 10));
    }
}
