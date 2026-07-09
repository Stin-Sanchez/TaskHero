package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.Prioridad;
import lombok.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa una misión (tarea) creada por un héroe.
 * <p>
 * Además del ciclo de vida básico (crear/editar/eliminar/completar), modela un
 * cronómetro persistente tipo Pomodoro: en vez de que el cliente cuente el
 * tiempo localmente (lo que se pierde al recargar la página o cambiar de
 * pestaña), el "reloj" vive en el servidor mediante {@code timerIniciadoEn}
 * (marca de tiempo de arranque) + {@code tiempoInvertidoSegundos} (total ya
 * acumulado en pausas anteriores). El total real en cualquier instante se
 * calcula con {@link #getTiempoTotalSegundos()}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarea {
    private Long id;
    private Long usuarioId;
    private String titulo;
    private String descripcion;
    private Prioridad prioridad;
    private LocalDate fechaLimite;
    private boolean completada;
    private String categoria;
    @Builder.Default
    private long tiempoInvertidoSegundos = 0;
    private LocalDateTime timerIniciadoEn;

    /**
     * Marca la tarea como completada (RF-08).
     * <p>
     * Si el cronómetro estaba corriendo, se pausa automáticamente primero para
     * que el tiempo invertido quede consolidado en {@code tiempoInvertidoSegundos}
     * antes de cerrar la misión; de lo contrario ese último tramo se perdería.
     */
    public void completar() {
        if (timerIniciadoEn != null) {
            pausarTimer();
        }
        this.completada = true;
    }

    /**
     * Arranca el cronómetro de la tarea, si no estaba ya corriendo.
     * <p>
     * Es idempotente a propósito: una segunda llamada mientras ya está activo no
     * reinicia {@code timerIniciadoEn} (eso descartaría el tiempo ya en curso).
     */
    public void iniciarTimer() {
        if (timerIniciadoEn == null) {
            timerIniciadoEn = LocalDateTime.now();
        }
    }

    /**
     * Pausa el cronómetro y consolida el tramo transcurrido.
     * <p>
     * Calcula el tiempo entre {@code timerIniciadoEn} y ahora, lo suma al total
     * acumulado, y limpia la marca de inicio (deja de estar "activo"). Llamarlo
     * cuando el cronómetro ya está pausado no tiene efecto.
     */
    public void pausarTimer() {
        if (timerIniciadoEn != null) {
            tiempoInvertidoSegundos += Duration.between(timerIniciadoEn, LocalDateTime.now()).getSeconds();
            timerIniciadoEn = null;
        }
    }

    /**
     * @return {@code true} si el cronómetro está corriendo actualmente (hay una marca de inicio pendiente de pausar).
     */
    public boolean isTimerActivo() {
        return timerIniciadoEn != null;
    }

    /**
     * Calcula el tiempo total invertido en la tarea al momento de la llamada.
     * <p>
     * Si el cronómetro está activo, suma el tramo en curso (ahora menos
     * {@code timerIniciadoEn}) al total ya acumulado, sin necesidad de pausar.
     * Esto es lo que permite que el frontend muestre el conteo en vivo sin que
     * el servidor tenga que escribir en base de datos cada segundo: el cliente
     * solo necesita este valor una vez y sigue incrementándolo localmente.
     *
     * @return el total de segundos invertidos hasta este instante.
     */
    public long getTiempoTotalSegundos() {
        if (timerIniciadoEn == null) {
            return tiempoInvertidoSegundos;
        }
        return tiempoInvertidoSegundos + Duration.between(timerIniciadoEn, LocalDateTime.now()).getSeconds();
    }
}
