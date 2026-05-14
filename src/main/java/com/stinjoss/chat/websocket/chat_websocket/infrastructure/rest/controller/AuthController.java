package com.stinjoss.chat.websocket.chat_websocket.infrastructure.rest.controller;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.AuthResponse;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.LoginRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.RegistroRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.UsuarioResponse;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.AutenticarUsuarioUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.RegistrarUsuarioUseCase;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Para desarrollo con frontend vanilla
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;
    private final com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository usuarioRepository;

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return new ResponseEntity<>(
                UsuarioResponse.fromDomain(registrarUsuarioUseCase.registrar(request)),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(autenticarUsuarioUseCase.autenticar(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<UsuarioResponse> getProfile() {
        String email = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(UsuarioResponse.fromDomain(usuario));
    }

    @GetMapping("/heroes")
    public ResponseEntity<List<UsuarioResponse>> getAllHeroes() {
        String myEmail = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<UsuarioResponse> heroes = usuarioRepository.findAll().stream()
                .filter(u -> !u.getEmail().equals(myEmail))
                .map(UsuarioResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(heroes);
    }
}
