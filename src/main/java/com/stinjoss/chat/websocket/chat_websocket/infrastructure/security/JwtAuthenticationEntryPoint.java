package com.stinjoss.chat.websocket.chat_websocket.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.rest.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) 
            throws IOException {
        
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("No estás autorizado para acceder a este recurso. Por favor, inicia sesión.")
                .path(request.getRequestURI())
                .build();

        final ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Para manejar LocalDateTime
        mapper.writeValue(response.getOutputStream(), apiError);
    }
}
