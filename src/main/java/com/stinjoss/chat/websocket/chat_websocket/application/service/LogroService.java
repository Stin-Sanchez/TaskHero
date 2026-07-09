package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.dto.LogroResponse;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.LogroUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.NotificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.UsuarioLogro;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.CodigoLogro;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.Prioridad;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.TareaRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioLogroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sistema de logros (achievements) de TaskHero (RF-18): un catálogo fijo de
 * hitos definidos como enum ({@link CodigoLogro}), evaluados contra el estado
 * actual del usuario cada vez que gana XP (ver {@link GamificationService}).
 * <p>
 * Solo se persiste qué logros ya obtuvo cada usuario y cuándo
 * ({@link UsuarioLogro}); el catálogo en sí (nombre, descripción, ícono,
 * umbral) vive en código porque es fijo y no necesita administrarse desde
 * base de datos.
 */
@Service
@RequiredArgsConstructor
public class LogroService implements LogroUseCase {

    private static final int VETERANO_TAREAS_REQUERIDAS = 25;
    private static final int LEYENDA_TAREAS_REQUERIDAS = 100;
    private static final int ESTRATEGA_TAREAS_ALTA_REQUERIDAS = 10;
    private static final long CRONOMETRISTA_SEGUNDOS_REQUERIDOS = 5 * 3600L;
    private static final int RACHA_SEMANAL_REQUERIDA = 7;
    private static final int RACHA_MENSUAL_REQUERIDA = 30;
    private static final int NIVEL_COMANDANTE = 5;
    private static final int NIVEL_MAESTRO = 8;

    private final TareaRepository tareaRepository;
    private final UsuarioLogroRepository usuarioLogroRepository;
    private final NotificationUseCase notificationUseCase;
    private final EmailPort emailPort;

    /**
     * Recalcula, a partir del estado actual del usuario y sus tareas, qué
     * logros ya se cumplen, y desbloquea únicamente los que aún no tenía
     * registrados (idempotente: llamarlo repetidamente no genera duplicados
     * ni vuelve a notificar un logro ya obtenido).
     * <p>
     * Se apoya en el conjunto completo de tareas del usuario para derivar
     * contadores (completadas, completadas de prioridad Alta, tiempo total de
     * cronómetro, "día perfecto"); es intencionalmente un recálculo completo
     * en cada llamada en vez de mantener contadores incrementales, ya que el
     * volumen de tareas de un usuario es pequeño y esto evita que los
     * contadores se desincronicen del estado real.
     */
    @Override
    @Transactional
    public void evaluarLogros(Usuario usuario) {
        Long usuarioId = usuario.getId();
        List<Tarea> tareas = tareaRepository.findByUsuarioId(usuarioId);
        long completadas = tareas.stream().filter(Tarea::isCompletada).count();
        long completadasAlta = tareas.stream()
                .filter(t -> t.isCompletada() && t.getPrioridad() == Prioridad.ALTA)
                .count();
        long segundosInvertidos = tareas.stream().mapToLong(Tarea::getTiempoInvertidoSegundos).sum();

        LocalDate hoy = LocalDate.now();
        List<Tarea> tareasHoy = tareas.stream().filter(t -> hoy.equals(t.getFechaLimite())).toList();
        boolean diaPerfecto = !tareasHoy.isEmpty() && tareasHoy.stream().allMatch(Tarea::isCompletada);

        Set<CodigoLogro> cumplidos = EnumSet.noneOf(CodigoLogro.class);
        if (completadas >= 1) cumplidos.add(CodigoLogro.PRIMERA_MISION);
        if (completadas >= VETERANO_TAREAS_REQUERIDAS) cumplidos.add(CodigoLogro.VETERANO_TAREAS);
        if (completadas >= LEYENDA_TAREAS_REQUERIDAS) cumplidos.add(CodigoLogro.LEYENDA_TAREAS);
        if (completadasAlta >= ESTRATEGA_TAREAS_ALTA_REQUERIDAS) cumplidos.add(CodigoLogro.ESTRATEGA);
        if (diaPerfecto) cumplidos.add(CodigoLogro.DIA_PERFECTO);
        if (segundosInvertidos >= CRONOMETRISTA_SEGUNDOS_REQUERIDOS) cumplidos.add(CodigoLogro.CRONOMETRISTA);
        if (usuario.getRachaDias() >= RACHA_SEMANAL_REQUERIDA) cumplidos.add(CodigoLogro.RACHA_SEMANAL);
        if (usuario.getRachaDias() >= RACHA_MENSUAL_REQUERIDA) cumplidos.add(CodigoLogro.RACHA_MENSUAL);
        if (usuario.getNivelActual() >= NIVEL_COMANDANTE) cumplidos.add(CodigoLogro.ASCENSO_COMANDANTE);
        if (usuario.getNivelActual() >= NIVEL_MAESTRO) cumplidos.add(CodigoLogro.ASCENSO_MAESTRO);

        for (CodigoLogro codigo : cumplidos) {
            if (!usuarioLogroRepository.existsByUsuarioIdAndCodigo(usuarioId, codigo)) {
                desbloquear(usuario, codigo);
            }
        }
    }

    /**
     * Registra un logro como obtenido y lo notifica al usuario reutilizando el
     * mismo canal de notificación in-app y de correo ({@code LOGRO_DESBLOQUEADO})
     * que ya usa el motor de gamificación para las subidas de nivel.
     */
    private void desbloquear(Usuario usuario, CodigoLogro codigo) {
        usuarioLogroRepository.save(UsuarioLogro.builder()
                .usuarioId(usuario.getId())
                .codigo(codigo)
                .fechaObtenido(LocalDateTime.now())
                .build());

        String mensaje = "¡Logro desbloqueado: " + codigo.getNombre() + "!";
        notificationUseCase.enviarNotificacion(usuario.getId(), TipoNotificacion.LOGRO_DESBLOQUEADO, mensaje);
        emailPort.enviarCorreoLogro(usuario.getEmail(), mensaje + " " + codigo.getDescripcion());
    }

    /**
     * Devuelve el catálogo completo de logros con su estado para este usuario
     * (bloqueado/desbloqueado y, si aplica, la fecha en que se obtuvo), para
     * alimentar la "Sala de Trofeos" del frontend.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LogroResponse> listarLogros(Long usuarioId) {
        Map<CodigoLogro, LocalDateTime> obtenidos = usuarioLogroRepository.findByUsuarioId(usuarioId).stream()
                .collect(Collectors.toMap(UsuarioLogro::getCodigo, UsuarioLogro::getFechaObtenido));

        return java.util.Arrays.stream(CodigoLogro.values())
                .map(codigo -> LogroResponse.builder()
                        .codigo(codigo.name())
                        .nombre(codigo.getNombre())
                        .descripcion(codigo.getDescripcion())
                        .icono(codigo.getIcono())
                        .desbloqueado(obtenidos.containsKey(codigo))
                        .fechaObtenido(obtenidos.get(codigo))
                        .build())
                .collect(Collectors.toList());
    }
}
