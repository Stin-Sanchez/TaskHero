package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
    private Long id;
    private String nombre;
    private String email;
    private String passwordHash;
    private int xpTotal;
    private int nivelActual;
    private int rachaDias;
    private boolean isPremium;
    private LocalDateTime ultimoLogin;
    private List<Long> amigosIds;

    /**
     * Regla de Negocio: Sumar XP y retornar si subió de nivel.
     */
    public boolean sumarXP(int cantidad, List<Nivel> nivelesDisponibles) {
        this.xpTotal += cantidad;
        return verificarSubidaNivel(nivelesDisponibles);
    }

    private boolean verificarSubidaNivel(List<Nivel> niveles) {
        int nivelAnterior = this.nivelActual;
        
        // Buscamos el nivel más alto que el usuario puede alcanzar con su XP actual
        niveles.stream()
                .filter(n -> this.xpTotal >= n.getXpRequerido())
                .mapToInt(Nivel::getNumero)
                .max()
                .ifPresent(nuevoNivel -> this.nivelActual = Math.max(this.nivelActual, nuevoNivel));

        return this.nivelActual > nivelAnterior;
    }

    public void actualizarRacha() {
        // Lógica para incrementar o resetear racha basada en la fecha actual vs ultimoLogin
        this.rachaDias++;
    }
}
