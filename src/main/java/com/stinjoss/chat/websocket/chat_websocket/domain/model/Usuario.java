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
    private String resetPasswordToken;
    private LocalDateTime tokenExpiration;
    private String avatarUrl;

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
        if (ultimoLogin == null) {
            this.rachaDias = 1;
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        long diasDiferencia = java.time.temporal.ChronoUnit.DAYS.between(ultimoLogin.toLocalDate(), ahora.toLocalDate());

        if (diasDiferencia == 1) {
            // Logueó ayer, incrementamos racha
            this.rachaDias++;
        } else if (diasDiferencia > 1) {
            // Pasó más de un día, perdio la racha
            this.rachaDias = 1;
        }
        // Si diasDiferencia == 0 (mismo día), no hacemos nada con el contador
    }

    public boolean esPrimerLoginDelDia() {
        if (ultimoLogin == null) return true;
        return !ultimoLogin.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }
}
