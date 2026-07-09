package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad de dominio que representa a un héroe (usuario) de TaskHero.
 * <p>
 * Concentra las reglas de negocio del sistema de gamificación: progreso de
 * experiencia (XP), subida de nivel y racha diaria de productividad. No tiene
 * dependencias de persistencia ni de framework (Arquitectura Hexagonal): los
 * adaptadores de infraestructura son responsables de mapear esta clase hacia
 * y desde su representación en base de datos.
 */
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
     * Acredita experiencia al héroe y evalúa si eso alcanza para subir de nivel.
     * <p>
     * Regla de negocio (RF-11/RF-12): el nivel nunca retrocede aunque el XP se
     * recalculara con un catálogo distinto; {@link #verificarSubidaNivel} solo
     * puede aumentar {@code nivelActual}, nunca disminuirlo.
     *
     * @param cantidad            XP a sumar (ya calculado por el llamador según la acción realizada).
     * @param nivelesDisponibles  catálogo completo de niveles, usado para determinar el nivel alcanzable.
     * @return {@code true} si el héroe subió de nivel como resultado de este XP.
     */
    public boolean sumarXP(int cantidad, List<Nivel> nivelesDisponibles) {
        this.xpTotal += cantidad;
        return verificarSubidaNivel(nivelesDisponibles);
    }

    /**
     * Determina el nivel más alto que el XP actual permite alcanzar y lo aplica.
     * <p>
     * Se usa {@code Math.max} contra el nivel actual como salvaguarda: aunque el
     * catálogo de niveles cambiara, esta operación nunca degrada al usuario.
     *
     * @param niveles catálogo de niveles (número y XP requerido).
     * @return {@code true} si el nivel resultante es mayor al que tenía antes de la llamada.
     */
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

    /**
     * Actualiza la racha de días consecutivos con actividad (RF-11), a invocarse
     * en cada login del día (ver {@link #esPrimerLoginDelDia()}).
     * <p>
     * Reglas de negocio:
     * <ul>
     *   <li>Sin login previo: es el primer día de racha (rachaDias = 1).</li>
     *   <li>Login el día inmediatamente anterior: la racha se incrementa.</li>
     *   <li>Más de un día sin actividad: la racha se reinicia a 1 (se "perdió").</li>
     *   <li>Mismo día: no se modifica (evita inflar la racha con múltiples logins diarios).</li>
     * </ul>
     * No actualiza {@code ultimoLogin}; esa responsabilidad es del llamador, para
     * mantener esta clase libre de acoplarse al reloj de la infraestructura.
     */
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

    /**
     * Indica si hoy aún no se ha procesado el login diario de este héroe.
     * <p>
     * Se usa como guarda para evitar otorgar el bono de XP por login más de una
     * vez por día natural, sin importar cuántas veces el usuario inicie sesión.
     *
     * @return {@code true} si nunca ha iniciado sesión o si su último login fue en una fecha distinta a hoy.
     */
    public boolean esPrimerLoginDelDia() {
        if (ultimoLogin == null) return true;
        return !ultimoLogin.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }
}
