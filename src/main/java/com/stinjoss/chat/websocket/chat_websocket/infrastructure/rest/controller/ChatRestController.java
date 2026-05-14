package com.stinjoss.chat.websocket.chat_websocket.infrastructure.rest.controller;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.GrupoRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.ChatUseCase;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.GrupoChat;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Mensaje;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatRestController {

    private final ChatUseCase chatUseCase;
    private final UsuarioRepository usuarioRepository;

    private Long getAuthenticatedUserId() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getId)
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
    }

    @GetMapping("/privado/{receptorId}")
    public ResponseEntity<List<Mensaje>> obtenerHistorialPrivado(@PathVariable Long receptorId) {
        return ResponseEntity.ok(chatUseCase.obtenerHistorialPrivado(getAuthenticatedUserId(), receptorId));
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<Mensaje>> obtenerHistorialGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(chatUseCase.obtenerHistorialGrupo(grupoId));
    }

    @PostMapping("/grupos")
    public ResponseEntity<GrupoChat> crearGrupo(@RequestBody GrupoRequest request) {
        return ResponseEntity.ok(chatUseCase.crearGrupo(request.getNombre(), getAuthenticatedUserId(), request.getMiembrosIds()));
    }

    @GetMapping("/grupos")
    public ResponseEntity<List<GrupoChat>> listarGrupos() {
        return ResponseEntity.ok(chatUseCase.listarGruposPorUsuario(getAuthenticatedUserId()));
    }
}
