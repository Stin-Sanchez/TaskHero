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

/**
 * Caso de uso de gestión de misiones (tareas): CRUD, finalización y cronómetro.
 * <p>
 * Todas las operaciones de lectura/escritura sobre una tarea puntual verifican
 * que pertenezca al usuario autenticado (ver {@link #buscarTareaPropia}); esto
 * evita que un usuario pueda editar, completar o controlar el cronómetro de
 * tareas ajenas simplemente adivinando su ID.
 */
@Service
@RequiredArgsConstructor
public class TareaService implements GestionarTareaUseCase {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GamificationUseCase gamificationUseCase;

    private static final int NIVEL_REQUERIDO_AVANZADO = 3;

    /**
     * Crea una nueva misión para el usuario, previa validación de nivel (RF-05).
     *
     * @throws com.stinjoss.chat.websocket.chat_websocket.domain.exception.InsufficientLevelException
     *         si el usuario intenta usar prioridad ALTA o una categoría personalizada sin el nivel requerido.
     */
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

    /**
     * Edita una misión existente del usuario (RF-06), re-validando el nivel por
     * si el cambio introduce una prioridad/categoría restringida.
     *
     * @throws ResourceNotFoundException si la tarea no existe o no pertenece al usuario.
     */
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

    /**
     * Regla de negocio de progresión (RF-13): antes del Nivel {@value #NIVEL_REQUERIDO_AVANZADO},
     * el héroe solo puede crear/editar tareas con prioridad Baja/Media y sin
     * categoría personalizada (o "General"). Prioridad ALTA y categorías propias
     * son funciones que se desbloquean al avanzar en el juego.
     */
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

    /**
     * Calcula estadísticas agregadas de las tareas del usuario (RF-18).
     * <p>
     * Función avanzada desbloqueada solo a partir de Nivel 10, consistente con
     * la tabla de "Funciones desbloqueables por nivel" del sistema de gamificación.
     *
     * @throws com.stinjoss.chat.websocket.chat_websocket.domain.exception.InsufficientLevelException
     *         si el usuario aún no alcanza el Nivel 10.
     */
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

    /**
     * Marca una misión como completada y dispara el motor de gamificación (RF-08).
     * <p>
     * Es la puerta de entrada principal al ciclo de recompensas: además de
     * cerrar la tarea (lo que auto-pausa su cronómetro, ver {@link Tarea#completar()}),
     * delega en {@link GamificationUseCase#procesarXpPorTareaCompletada} el
     * cálculo de XP, la posible subida de nivel y la evaluación de logros.
     *
     * @throws ResourceNotFoundException si la tarea no existe o no es del usuario.
     * @throws DomainException si la tarea ya estaba completada (evita otorgar XP dos veces por la misma misión).
     */
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

    /**
     * Busca una tarea verificando que pertenezca al usuario indicado.
     * <p>
     * Punto único de control de propiedad para las operaciones del cronómetro;
     * evita que un usuario controle el cronómetro de una tarea que no es suya.
     *
     * @throws ResourceNotFoundException si la tarea no existe o no le pertenece.
     */
    private Tarea buscarTareaPropia(Long usuarioId, Long tareaId) {
        return tareaRepository.findById(tareaId)
                .filter(t -> t.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));
    }

    /**
     * Inicia el cronómetro de una tarea propia y pendiente.
     *
     * @throws DomainException si la tarea ya está completada (no tiene sentido cronometrar una misión cerrada).
     */
    @Override
    @Transactional
    public Tarea iniciarTimer(Long usuarioId, Long tareaId) {
        Tarea tarea = buscarTareaPropia(usuarioId, tareaId);
        if (tarea.isCompletada()) {
            throw new DomainException("No se puede iniciar el cronómetro de una tarea completada");
        }
        tarea.iniciarTimer();
        return tareaRepository.save(tarea);
    }

    /**
     * Pausa el cronómetro de una tarea propia, consolidando el tiempo transcurrido.
     */
    @Override
    @Transactional
    public Tarea pausarTimer(Long usuarioId, Long tareaId) {
        Tarea tarea = buscarTareaPropia(usuarioId, tareaId);
        tarea.pausarTimer();
        return tareaRepository.save(tarea);
    }
}
