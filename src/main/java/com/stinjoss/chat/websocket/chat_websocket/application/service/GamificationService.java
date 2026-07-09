package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GamificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.LogroUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.NotificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Nivel;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.NivelRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.TareaRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Motor de gamificación del sistema (RF-11/RF-12/RF-13): calcula el XP ganado
 * por las acciones del héroe, evalúa subidas de nivel, otorga el bono diario
 * de constancia y dispara la evaluación de logros ({@link LogroUseCase}).
 * <p>
 * Es el punto central al que convergen los dos eventos que generan progreso:
 * completar una tarea ({@link #procesarXpPorTareaCompletada}) e iniciar sesión
 * en el día ({@link #procesarLoginDiario}).
 */
@Service
@RequiredArgsConstructor
public class GamificationService implements GamificationUseCase {

    private final UsuarioRepository usuarioRepository;
    private final NivelRepository nivelRepository;
    private final TareaRepository tareaRepository;
    private final NotificationUseCase notificationUseCase;
    private final NotificacionRepository notificacionRepository;
    private final EmailPort emailPort;
    private final LogroUseCase logroUseCase;

    private static final int XP_BASE = 10;
    private static final int XP_LOGIN_DIARIO = 5;
    private static final int XP_MANTENER_RACHA = 20;
    private static final int XP_DIA_COMPLETADO = 50;

    /**
     * Procesa el XP correspondiente a completar una tarea y encadena el resto
     * del ciclo de recompensas.
     * <p>
     * Orden de la lógica:
     * <ol>
     *   <li>El XP base depende de la prioridad de la última tarea completada
     *       (Baja=10, Media=20, Alta=30) — recompensa proporcional al esfuerzo.</li>
     *   <li>Se evalúa el bono de "Día Perfecto" (todas las tareas con vencimiento
     *       hoy completadas), limitado a una vez por día mediante
     *       {@link #haRecibidoBonoHoy} para que no se otorgue repetidamente si el
     *       usuario completa varias tareas el mismo día tras ya haberlo recibido.</li>
     *   <li>Si el XP acumulado alcanza el siguiente nivel, se notifica la subida.</li>
     *   <li>Se delega en {@link LogroUseCase#evaluarLogros} para desbloquear
     *       cualquier logro cuya condición ya se cumpla (racha, total de tareas, etc.).</li>
     * </ol>
     *
     * @throws ResourceNotFoundException si el usuario no existe.
     */
    @Override
    @Transactional
    public void procesarXpPorTareaCompletada(Long usuarioId) {
        // ... (resto del inicio igual)
        List<Tarea> tareas = tareaRepository.findByUsuarioId(usuarioId);
        Tarea ultimaCompletada = tareas.stream()
                .filter(Tarea::isCompletada)
                .sorted((t1, t2) -> t2.getId().compareTo(t1.getId())) // Asumimos ID incremental
                .findFirst()
                .orElse(null);

        int xpAGanar = XP_BASE;
        if (ultimaCompletada != null && ultimaCompletada.getPrioridad() != null) {
            switch (ultimaCompletada.getPrioridad()) {
                case MEDIA -> xpAGanar = XP_BASE * 2;
                case ALTA -> xpAGanar = XP_BASE * 3;
                default -> xpAGanar = XP_BASE;
            }
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<Nivel> niveles = nivelRepository.findAll();
        
        boolean subioDeNivel = usuario.sumarXP(xpAGanar, niveles);
        
        notificationUseCase.enviarNotificacion(usuarioId, TipoNotificacion.TAREA_COMPLETADA, 
                "¡Has ganado " + xpAGanar + " XP por completar una misión " + 
                (ultimaCompletada != null ? ultimaCompletada.getPrioridad() : "BAJA") + "!");

        // Bono: ¿Completó todas las tareas de hoy? ¿Y no ha recibido el bono ya hoy?
        if (verificarBonoDiaCompletado(usuarioId) && !haRecibidoBonoHoy(usuarioId)) {
            subioDeNivel = usuario.sumarXP(XP_DIA_COMPLETADO, niveles) || subioDeNivel;
            notificationUseCase.enviarNotificacion(usuarioId, TipoNotificacion.LOGRO_DESBLOQUEADO, 
                    "¡Bono de Constancia! +50 XP por limpiar tu tablero de hoy.");
        }

        usuarioRepository.save(usuario);

        if (subioDeNivel) {
            notificarSubidaNivel(usuario);
        }

        logroUseCase.evaluarLogros(usuario);
    }

    /**
     * Evita otorgar el bono de "Día Perfecto" más de una vez el mismo día,
     * usando el propio historial de notificaciones como registro de control
     * (no existe una tabla dedicada a bonos ya entregados).
     */
    private boolean haRecibidoBonoHoy(Long usuarioId) {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        return notificacionRepository.existsByUsuarioIdAndTipoAndFechaCreacionAfter(
                usuarioId, TipoNotificacion.LOGRO_DESBLOQUEADO, inicioHoy);
    }

    /**
     * Determina si el usuario completó absolutamente todas las tareas cuya
     * fecha límite es hoy (el "tablero del día"). Un usuario sin tareas para
     * hoy no califica para el bono (no hay "tablero" que limpiar).
     */
    private boolean verificarBonoDiaCompletado(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        List<Tarea> tareasHoy = tareaRepository.findByUsuarioId(usuarioId).stream()
                .filter(t -> t.getFechaLimite() != null && t.getFechaLimite().equals(hoy))
                .toList();

        if (tareasHoy.isEmpty()) return false;

        return tareasHoy.stream().allMatch(Tarea::isCompletada);
    }

    /**
     * Procesa el bono de racha diaria (RF-11) la primera vez que el usuario
     * inicia sesión en el día (ver {@link Usuario#esPrimerLoginDelDia()}).
     * <p>
     * El XP otorgado es mayor si ya mantenía una racha activa
     * ({@value #XP_MANTENER_RACHA}) que si es su primer login
     * ({@value #XP_LOGIN_DIARIO}), para incentivar la constancia día tras día.
     * Logins adicionales el mismo día no generan XP ni afectan la racha.
     *
     * @throws ResourceNotFoundException si el usuario no existe.
     */
    @Override
    @Transactional
    public void procesarLoginDiario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!usuario.esPrimerLoginDelDia()) {
            return; 
        }

        usuario.actualizarRacha();
        List<Nivel> niveles = nivelRepository.findAll();
        
        int xpAGanar = (usuario.getRachaDias() > 1) ? XP_MANTENER_RACHA : XP_LOGIN_DIARIO;
        boolean subioDeNivel = usuario.sumarXP(xpAGanar, niveles);
        
        usuario.setUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);

        notificationUseCase.enviarNotificacion(usuarioId, TipoNotificacion.RACHA_MANTENIDA, 
                "¡Login diario! XP ganado: " + xpAGanar + ". Racha actual: " + usuario.getRachaDias() + " días.");
        
        if (subioDeNivel) {
            notificarSubidaNivel(usuario);
        }

        logroUseCase.evaluarLogros(usuario);
    }

    /**
     * Notifica la subida de nivel tanto dentro de la aplicación (in-app) como
     * por correo electrónico (RF-15), reutilizando el mismo canal de "logro"
     * usado por el módulo de {@link LogroUseCase}.
     */
    private void notificarSubidaNivel(Usuario usuario) {
        String msg = "¡Felicidades! Has alcanzado el nivel " + usuario.getNivelActual();
        notificationUseCase.enviarNotificacion(usuario.getId(), TipoNotificacion.NIVEL_SUBIDO, msg);
        emailPort.enviarCorreoLogro(usuario.getEmail(), msg);
    }
}
