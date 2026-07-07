package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GamificationUseCase;
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

@Service
@RequiredArgsConstructor
public class GamificationService implements GamificationUseCase {

    private final UsuarioRepository usuarioRepository;
    private final NivelRepository nivelRepository;
    private final TareaRepository tareaRepository;
    private final NotificationUseCase notificationUseCase;
    private final NotificacionRepository notificacionRepository;
    private final EmailPort emailPort;

    private static final int XP_BASE = 10;
    private static final int XP_LOGIN_DIARIO = 5;
    private static final int XP_MANTENER_RACHA = 20;
    private static final int XP_DIA_COMPLETADO = 50;

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
    }

    private boolean haRecibidoBonoHoy(Long usuarioId) {
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        return notificacionRepository.existsByUsuarioIdAndTipoAndFechaCreacionAfter(
                usuarioId, TipoNotificacion.LOGRO_DESBLOQUEADO, inicioHoy);
    }

    private boolean verificarBonoDiaCompletado(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        List<Tarea> tareasHoy = tareaRepository.findByUsuarioId(usuarioId).stream()
                .filter(t -> t.getFechaLimite() != null && t.getFechaLimite().equals(hoy))
                .toList();

        if (tareasHoy.isEmpty()) return false;

        return tareasHoy.stream().allMatch(Tarea::isCompletada);
    }

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
    }

    private void notificarSubidaNivel(Usuario usuario) {
        String msg = "¡Felicidades! Has alcanzado el nivel " + usuario.getNivelActual();
        notificationUseCase.enviarNotificacion(usuario.getId(), TipoNotificacion.NIVEL_SUBIDO, msg);
        emailPort.enviarCorreoLogro(usuario.getEmail(), msg);
    }
}
