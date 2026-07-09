package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.port.in.NotificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.TareaRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Job programado de recordatorios de vencimiento de tareas (RF-15).
 * <p>
 * Corre una vez al día y es el único punto del sistema que recorre todos los
 * usuarios de forma masiva (el resto de los servicios siempre operan sobre un
 * usuario autenticado puntual).
 */
@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private final UsuarioRepository usuarioRepository;
    private final TareaRepository tareaRepository;
    private final NotificationUseCase notificationUseCase;
    private final EmailPort emailPort;

    /**
     * Revisa, para cada usuario, sus tareas pendientes con fecha límite hoy o
     * mañana, y envía un recordatorio (notificación in-app + correo) por cada
     * una. Una misma tarea genera como máximo dos recordatorios en su vida
     * (uno el día anterior al vencimiento y otro el día del vencimiento), ya
     * que cada ejecución diaria solo la ve mientras estas dos condiciones se
     * cumplan.
     *
     * // ponytail: recorre usuario por usuario con findByUsuarioId ya existente en vez de agregar
     * // una query global por fecha; a escala de proyecto academico es suficiente, si el volumen
     * // de usuarios/tareas crece conviene un TareaRepository.findByFechaLimiteAndCompletadaFalse().
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void enviarRecordatoriosDeVencimiento() {
        LocalDate hoy = LocalDate.now();
        LocalDate manana = hoy.plusDays(1);

        for (Usuario usuario : usuarioRepository.findAll()) {
            List<Tarea> proximasAVencer = tareaRepository.findByUsuarioId(usuario.getId()).stream()
                    .filter(t -> !t.isCompletada())
                    .filter(t -> t.getFechaLimite() != null)
                    .filter(t -> t.getFechaLimite().equals(hoy) || t.getFechaLimite().equals(manana))
                    .toList();

            for (Tarea tarea : proximasAVencer) {
                String plazo = tarea.getFechaLimite().equals(hoy) ? "hoy" : "mañana";
                String mensaje = "Tu misión \"" + tarea.getTitulo() + "\" vence " + plazo + ".";
                notificationUseCase.enviarNotificacion(usuario.getId(), TipoNotificacion.SISTEMA, mensaje);
                emailPort.enviarCorreoRecordatorio(usuario.getEmail(), mensaje);
            }
        }
    }
}
