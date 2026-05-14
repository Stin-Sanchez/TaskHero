package com.stinjoss.chat.websocket.chat_websocket.infrastructure.rest.controller;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.TareaRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.TareaResponse;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GestionarTareaUseCase;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TareaController {

    private final GestionarTareaUseCase gestionarTareaUseCase;
    private final UsuarioRepository usuarioRepository;

    private Long getAuthenticatedUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getId)
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
    }

    @PostMapping
    public ResponseEntity<TareaResponse> crear(@Valid @RequestBody TareaRequest request) {
        return new ResponseEntity<>(
                TareaResponse.fromDomain(gestionarTareaUseCase.crearTarea(getAuthenticatedUserId(), request)),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<List<TareaResponse>> listar() {
        List<TareaResponse> response = gestionarTareaUseCase.listarTareasPorUsuario(getAuthenticatedUserId())
                .stream()
                .map(TareaResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TareaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody TareaRequest request) {
        return ResponseEntity.ok(
                TareaResponse.fromDomain(gestionarTareaUseCase.actualizarTarea(getAuthenticatedUserId(), id, request))
        );
    }

    @PatchMapping("/{id}/completar")
    public ResponseEntity<TareaResponse> completar(@PathVariable Long id) {
        return ResponseEntity.ok(
                TareaResponse.fromDomain(gestionarTareaUseCase.marcarComoCompletada(getAuthenticatedUserId(), id))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        gestionarTareaUseCase.eliminarTarea(getAuthenticatedUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
