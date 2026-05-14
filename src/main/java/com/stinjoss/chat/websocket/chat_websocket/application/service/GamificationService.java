package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.port.in.GamificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.NotificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Nivel;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.NivelRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GamificationService implements GamificationUseCase {

    private final UsuarioRepository usuarioRepository;
    private final NivelRepository nivelRepository;
    private final NotificationUseCase notificationUseCase;
    private final EmailPort emailPort;

    private static final int XP_TAREA_COMPLETADA = 10;
    private static final int XP_LOGIN_DIARIO = 5;

    @Override
    @Transactional
    public void procesarXpPorTareaCompletada(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<Nivel> niveles = nivelRepository.findAll();
        
        boolean subioDeNivel = usuario.sumarXP(XP_TAREA_COMPLETADA, niveles);
        usuarioRepository.save(usuario);

        // Crear notificación de XP ganado
        notificationUseCase.enviarNotificacion(usuarioId, TipoNotificacion.TAREA_COMPLETADA, 
                "¡Has ganado " + XP_TAREA_COMPLETADA + " XP por completar una tarea!");

        if (subioDeNivel) {
            String msg = "¡Felicidades! Has alcanzado el nivel " + usuario.getNivelActual();
            notificationUseCase.enviarNotificacion(usuarioId, TipoNotificacion.NIVEL_SUBIDO, msg);
            emailPort.enviarCorreoLogro(usuario.getEmail(), msg);
        }
    }

    @Override
    @Transactional
    public void procesarLoginDiario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        usuario.actualizarRacha();
        List<Nivel> niveles = nivelRepository.findAll();
        
        boolean subioDeNivel = usuario.sumarXP(XP_LOGIN_DIARIO, niveles);
        usuario.setUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);

        notificationUseCase.enviarNotificacion(usuarioId, TipoNotificacion.RACHA_MANTENIDA, 
                "¡Login diario! XP ganado: " + XP_LOGIN_DIARIO + ". Racha actual: " + usuario.getRachaDias() + " días.");
        
        if (subioDeNivel) {
            String msg = "¡Subiste de nivel por tu constancia! Nivel actual: " + usuario.getNivelActual();
            notificationUseCase.enviarNotificacion(usuarioId, TipoNotificacion.NIVEL_SUBIDO, msg);
            emailPort.enviarCorreoLogro(usuario.getEmail(), msg);
        }
    }
}
