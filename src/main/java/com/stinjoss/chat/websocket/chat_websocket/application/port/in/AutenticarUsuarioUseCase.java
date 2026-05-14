package com.stinjoss.chat.websocket.chat_websocket.application.port.in;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.LoginRequest;
import com.stinjoss.chat.websocket.chat_websocket.application.dto.AuthResponse;

public interface AutenticarUsuarioUseCase {
    AuthResponse autenticar(LoginRequest request);
}
