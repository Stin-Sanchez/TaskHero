package com.stinjoss.chat.websocket.chat_websocket.infrastructure.rest.controller;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Notificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.NotificacionRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    private Long getAuthenticatedUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getId)
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
    }

    @GetMapping
    public ResponseEntity<List<Notificacion>> listar() {
        return ResponseEntity.ok(notificacionRepository.findByUsuarioId(getAuthenticatedUserId()));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Long id) {
        notificacionRepository.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}
