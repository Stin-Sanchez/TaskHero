package com.stinjoss.chat.websocket.chat_websocket.infrastructure.rest.controller;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.LogroResponse;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.LogroUseCase;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logros")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LogroController {

    private final LogroUseCase logroUseCase;
    private final UsuarioRepository usuarioRepository;

    private Long getAuthenticatedUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getId)
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
    }

    @GetMapping
    public ResponseEntity<List<LogroResponse>> listar() {
        return ResponseEntity.ok(logroUseCase.listarLogros(getAuthenticatedUserId()));
    }
}
