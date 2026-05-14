package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.TareaRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GamificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GestionarTareaUseCase;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.DomainException;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.TareaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TareaService implements GestionarTareaUseCase {

    private final TareaRepository tareaRepository;
    private final GamificationUseCase gamificationUseCase;

    @Override
    @Transactional
    public Tarea crearTarea(Long usuarioId, TareaRequest request) {
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

        tareaExistente.setTitulo(request.getTitulo());
        tareaExistente.setDescripcion(request.getDescripcion());
        tareaExistente.setPrioridad(request.getPrioridad());
        tareaExistente.setFechaLimite(request.getFechaLimite());
        tareaExistente.setCategoria(request.getCategoria());

        return tareaRepository.save(tareaExistente);
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
