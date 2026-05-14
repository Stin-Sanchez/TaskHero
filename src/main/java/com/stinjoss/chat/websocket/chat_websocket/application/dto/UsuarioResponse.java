package com.stinjoss.chat.websocket.chat_websocket.application.dto;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String email;
    private int xpTotal;
    private int nivelActual;
    private int rachaDias;
    private boolean isPremium;
    private LocalDateTime ultimoLogin;
    private List<Long> amigosIds;

    public static UsuarioResponse fromDomain(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .xpTotal(usuario.getXpTotal())
                .nivelActual(usuario.getNivelActual())
                .rachaDias(usuario.getRachaDias())
                .isPremium(usuario.isPremium())
                .ultimoLogin(usuario.getUltimoLogin())
                .amigosIds(usuario.getAmigosIds())
                .build();
    }
}
