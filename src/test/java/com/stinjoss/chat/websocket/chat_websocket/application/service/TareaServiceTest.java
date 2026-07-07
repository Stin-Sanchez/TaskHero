package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.TareaRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GamificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.InsufficientLevelException;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.Prioridad;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.TareaRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TareaServiceTest {

    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private GamificationUseCase gamificationUseCase;

    @InjectMocks
    private TareaService tareaService;

    private Long userId = 1L;
    private Usuario usuarioNivel1;
    private Usuario usuarioNivel3;
    private Usuario usuarioNivel10;

    @BeforeEach
    void setUp() {
        usuarioNivel1 = Usuario.builder().id(userId).nivelActual(1).build();
        usuarioNivel3 = Usuario.builder().id(userId).nivelActual(3).build();
        usuarioNivel10 = Usuario.builder().id(userId).nivelActual(10).build();
    }

    @Test
    @DisplayName("Debe lanzar excepción si usuario Nivel 1 intenta usar prioridad ALTA")
    void crearTareaNivelBajoPrioridadAlta() {
        TareaRequest request = new TareaRequest();
        request.setTitulo("Test");
        request.setPrioridad(Prioridad.ALTA);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuarioNivel1));

        assertThrows(InsufficientLevelException.class, () -> tareaService.crearTarea(userId, request));
    }

    @Test
    @DisplayName("Debe permitir crear tarea con categoría si el usuario es Nivel 3")
    void crearTareaNivelAltoExito() {
        TareaRequest request = new TareaRequest();
        request.setTitulo("Test");
        request.setPrioridad(Prioridad.ALTA);
        request.setCategoria("Estudios");

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuarioNivel3));
        
        tareaService.crearTarea(userId, request);

        verify(tareaRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción al solicitar stats si es menor a Nivel 10")
    void obtenerStatsNivelBajo() {
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuarioNivel3));

        assertThrows(InsufficientLevelException.class, () -> tareaService.obtenerEstadisticas(userId));
    }

    @Test
    @DisplayName("Debe retornar estadísticas correctas para usuario Nivel 10")
    void obtenerStatsExito() {
        List<com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea> tareas = List.of(
                com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea.builder().prioridad(Prioridad.ALTA).completada(true).categoria("Trabajo").build(),
                com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea.builder().prioridad(Prioridad.BAJA).completada(false).categoria("Estudios").build()
        );

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuarioNivel10));
        when(tareaRepository.findByUsuarioId(userId)).thenReturn(tareas);

        var stats = tareaService.obtenerEstadisticas(userId);

        assertEquals(2, stats.getTotalTareas());
        assertEquals(1, stats.getCompletadas());
        assertEquals(50.0, stats.getPorcentajeCumplimiento());
        assertTrue(stats.getTareasPorPrioridad().containsKey("ALTA"));
        assertTrue(stats.getTareasPorCategoria().containsKey("Trabajo"));
    }
}
