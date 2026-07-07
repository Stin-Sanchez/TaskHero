package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.TareaRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GamificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GestionarTareaUseCase;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.DomainException;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.TareaRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TareaService implements GestionarTareaUseCase {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GamificationUseCase gamificationUseCase;

    private static final int NIVEL_REQUERIDO_AVANZADO = 3;

    @Override
    @Transactional
    public Tarea crearTarea(Long usuarioId, TareaRequest request) {
        validarRestriccionesNivel(usuarioId, request);

        Tarea nuevaTarea = Tarea.builder()
                .usuarioId(usuarioId)
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .prioridad(request.getPrioridad())
                .fechaLimite(request.getFechaLimite())
                .categoria(request.getCategoria())
                .completada(false)
                .build();

        return tareaRepository.save(nuevaTarea);
    }

    @Override
    @Transactional
    public Tarea actualizarTarea(Long usuarioId, Long tareaId, TareaRequest request) {
        Tarea tareaExistente = tareaRepository.findById(tareaId)
                .filter(t -> t.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada o no pertenece al usuario"));

        validarRestriccionesNivel(usuarioId, request);

        tareaExistente.setTitulo(request.getTitulo());
        tareaExistente.setDescripcion(request.getDescripcion());
        tareaExistente.setPrioridad(request.getPrioridad());
        tareaExistente.setFechaLimite(request.getFechaLimite());
        tareaExistente.setCategoria(request.getCategoria());

        return tareaRepository.save(tareaExistente);
    }

    private void validarRestriccionesNivel(Long usuarioId, TareaRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (usuario.getNivelActual() < NIVEL_REQUERIDO_AVANZADO) {
            boolean usaPrioridadAlta = com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.Prioridad.ALTA.equals(request.getPrioridad());
            boolean usaCategoriaEspecial = request.getCategoria() != null && 
                                          !request.getCategoria().isBlank() && 
                                          !request.getCategoria().equalsIgnoreCase("General");

            if (usaPrioridadAlta || usaCategoriaEspecial) {
                throw new com.stinjoss.chat.websocket.chat_websocket.domain.exception.InsufficientLevelException(
                    "Debes alcanzar el Nivel " + NIVEL_REQUERIDO_AVANZADO + " para usar categorías o prioridad ALTA.");
            }
        }
    }

    @Override
    @Transactional
    public void eliminarTarea(Long usuarioId, Long tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .filter(t -> t.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));
        
        tareaRepository.deleteById(tarea.getId());
    }

    @Override
    public List<Tarea> listarTareasPorUsuario(Long usuarioId) {
        return tareaRepository.findByUsuarioId(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public com.stinjoss.chat.websocket.chat_websocket.application.dto.EstadisticasResponse obtenerEstadisticas(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (usuario.getNivelActual() < 10) {
            throw new com.stinjoss.chat.websocket.chat_websocket.domain.exception.InsufficientLevelException("Las estadísticas avanzadas se desbloquean al Nivel 10.");
        }

        List<Tarea> tareas = tareaRepository.findByUsuarioId(usuarioId);
        long total = tareas.size();
        long completadas = tareas.stream().filter(Tarea::isCompletada).count();
        
        java.util.Map<String, Long> porPrioridad = tareas.stream()
                .collect(java.util.stream.Collectors.groupingBy(t -> t.getPrioridad().name(), java.util.stream.Collectors.counting()));

        java.util.Map<String, Long> porCategoria = tareas.stream()
                .filter(t -> t.getCategoria() != null)
                .collect(java.util.stream.Collectors.groupingBy(Tarea::getCategoria, java.util.stream.Collectors.counting()));

        return com.stinjoss.chat.websocket.chat_websocket.application.dto.EstadisticasResponse.builder()
                .totalTareas(total)
                .completadas(completadas)
                .pendientes(total - completadas)
                .porcentajeCumplimiento(total > 0 ? (double) completadas / total * 100 : 0)
                .tareasPorPrioridad(porPrioridad)
                .tareasPorCategoria(porCategoria)
                .build();
    }

    @Override
    @Transactional
    public Tarea marcarComoCompletada(Long usuarioId, Long tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .filter(t -> t.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));

        if (tarea.isCompletada()) {
            throw new DomainException("La tarea ya está completada");
        }

        tarea.completar(); 
        Tarea tareaGuardada = tareaRepository.save(tarea);

        // Disparar motor de gamificación
        gamificationUseCase.procesarXpPorTareaCompletada(usuarioId);

        return tareaGuardada;
    }
}
