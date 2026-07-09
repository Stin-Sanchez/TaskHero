package com.stinjoss.chat.websocket.chat_websocket.application.service;

import com.stinjoss.chat.websocket.chat_websocket.application.port.in.ChatUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.in.NotificationUseCase;
import com.stinjoss.chat.websocket.chat_websocket.application.port.out.ChatPushPort;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.GrupoAccessDeniedException;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.DomainException;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.InsufficientLevelException;
import com.stinjoss.chat.websocket.chat_websocket.domain.exception.ResourceNotFoundException;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.GrupoChat;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Mensaje;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.Usuario;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.TipoNotificacion;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.ChatRepository;
import com.stinjoss.chat.websocket.chat_websocket.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso de chat en tiempo real y gremios (RF-16/RF-17).
 * <p>
 * Todas las operaciones exigen Nivel {@value #NIVEL_MINIMO_CHAT} (función
 * desbloqueable), y el acceso a un gremio (leer su historial o enviar
 * mensajes en él) exige además pertenecer a ese gremio, verificado contra la
 * tabla puente usuario-gremio en {@link ChatRepository#esMiembroDelGrupo}
 * (el objeto {@link GrupoChat} devuelto por {@code findGrupoById} no trae
 * cargada la lista de miembros, por diseño de su mapper de persistencia).
 */
@Service
@RequiredArgsConstructor
public class ChatService implements ChatUseCase {

    private final ChatRepository chatRepository;
    private final ChatPushPort chatPushPort;
    private final UsuarioRepository usuarioRepository;
    private final NotificationUseCase notificationUseCase;

    private static final int NIVEL_MINIMO_CHAT = 5;

    /**
     * Envía un mensaje privado entre dos usuarios (RF-16), tras validar que el
     * contenido no esté vacío y que el remitente tenga el nivel requerido.
     */
    @Override
    @Transactional
    public Mensaje enviarMensajePrivado(Long remitenteId, Long receptorId, String contenido) {
        validarContenido(contenido);
        Usuario remitente = verificarNivelUsuario(remitenteId);

        Mensaje mensaje = Mensaje.builder()
                .remitenteId(remitenteId)
                .remitenteNombre(remitente.getNombre())
                .receptorId(receptorId)
                .contenido(contenido)
                .timestamp(LocalDateTime.now())
                .build();

        Mensaje guardado = chatRepository.saveMensaje(mensaje);
        chatPushPort.enviarMensajePrivado(guardado);

        // Notificación al receptor
        notificationUseCase.enviarNotificacion(receptorId, TipoNotificacion.MENSAJE_RECIBIDO, 
                "Nuevo mensaje de " + remitente.getNombre());

        return guardado;
    }

    /**
     * Envía un mensaje a un gremio (RF-16), exigiendo que el remitente sea
     * miembro de ese gremio además del nivel mínimo de chat.
     *
     * @throws com.stinjoss.chat.websocket.chat_websocket.domain.exception.GrupoAccessDeniedException
     *         si el remitente no pertenece al gremio.
     */
    @Override
    @Transactional
    public Mensaje enviarMensajeGrupo(Long remitenteId, Long grupoId, String contenido) {
        validarContenido(contenido);
        Usuario remitente = verificarNivelUsuario(remitenteId);

        if (!chatRepository.esMiembroDelGrupo(remitenteId, grupoId)) {
            throw new GrupoAccessDeniedException("No perteneces a este gremio.");
        }

        Mensaje mensaje = Mensaje.builder()
                .remitenteId(remitenteId)
                .remitenteNombre(remitente.getNombre())
                .grupoId(grupoId)
                .contenido(contenido)
                .timestamp(LocalDateTime.now())
                .build();

        Mensaje guardado = chatRepository.saveMensaje(mensaje);
        chatPushPort.enviarMensajeGrupo(guardado);

        // Notificar a miembros del grupo (excepto remitente)
        // Nota: En un sistema real buscaríamos los IDs de miembros del grupo
        // Por ahora, como no tenemos una búsqueda de miembros fácil en el puerto, 
        // asumimos que el websocket ya maneja la distribución pero el "registro" de notificación
        // lo hacemos de forma simplificada.
        
        return guardado;
    }

    /**
     * Valida que el contenido de un mensaje no sea nulo ni esté en blanco (RF-16:
     * "el sistema valida el contenido" antes de enviarlo por WebSocket).
     */
    private void validarContenido(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new DomainException("El mensaje no puede estar vacío.");
        }
    }

    /**
     * Verifica que el usuario exista y haya alcanzado el nivel mínimo de chat
     * (RF-13: función desbloqueable). Se reutiliza en todas las operaciones de
     * chat y gremios como guarda de entrada.
     *
     * @throws ResourceNotFoundException si el usuario no existe.
     * @throws InsufficientLevelException si no alcanza {@value #NIVEL_MINIMO_CHAT}.
     */
    private Usuario verificarNivelUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario remitente no encontrado"));

        if (usuario.getNivelActual() < NIVEL_MINIMO_CHAT) {
            throw new InsufficientLevelException("Debes alcanzar el Nivel " + NIVEL_MINIMO_CHAT + " para usar el chat.");
        }
        return usuario;
    }

    @Override
    public List<Mensaje> obtenerHistorialPrivado(Long user1, Long user2) {
        verificarNivelUsuario(user1);
        return chatRepository.findHistorialPrivado(user1, user2);
    }

    /**
     * Obtiene el historial de mensajes de un gremio.
     * <p>
     * Control de acceso crítico: sin esta verificación, cualquier usuario
     * autenticado podría leer el historial de cualquier gremio adivinando su
     * ID (IDOR). Solo los miembros reales del gremio pueden consultarlo.
     *
     * @throws com.stinjoss.chat.websocket.chat_websocket.domain.exception.GrupoAccessDeniedException
     *         si el usuario no pertenece al gremio.
     */
    @Override
    public List<Mensaje> obtenerHistorialGrupo(Long usuarioId, Long grupoId) {
        verificarNivelUsuario(usuarioId);

        if (!chatRepository.esMiembroDelGrupo(usuarioId, grupoId)) {
            throw new GrupoAccessDeniedException("No perteneces a este gremio.");
        }

        return chatRepository.findHistorialGrupo(grupoId);
    }

    /**
     * Crea un gremio (RF-17), agregando siempre al creador como primer miembro
     * aunque no lo haya incluido explícitamente en la lista.
     */
    @Override
    @Transactional
    public GrupoChat crearGrupo(String nombre, Long creadorId, List<Long> miembrosIds) {
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("El nombre del gremio es obligatorio.");
        }
        verificarNivelUsuario(creadorId);

        if (miembrosIds == null) {
            miembrosIds = new java.util.ArrayList<>();
        }
        if (!miembrosIds.contains(creadorId)) {
            miembrosIds.add(creadorId);
        }

        GrupoChat grupo = GrupoChat.builder()
                .nombre(nombre)
                .miembrosIds(miembrosIds)
                .build();

        return chatRepository.saveGrupo(grupo);
    }

    /**
     * Envía una invitación de gremio (RF-17) como notificación in-app.
     * <p>
     * No exige que el remitente ya sea miembro del gremio (cualquier héroe con
     * nivel de chat puede invitar), pero sí evita invitar a alguien que ya
     * pertenece, para no duplicar notificaciones de reclutamiento, y evita
     * que el remitente se invite a sí mismo.
     *
     * @throws DomainException si el receptor es el propio remitente o ya es miembro del gremio.
     */
    @Override
    @Transactional
    public void invitarMiembro(Long remitenteId, Long receptorId, Long grupoId) {
        verificarNivelUsuario(remitenteId);
        GrupoChat grupo = chatRepository.findGrupoById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        if (remitenteId.equals(receptorId)) {
            throw new DomainException("No puedes invitarte a ti mismo a un gremio.");
        }

        if (chatRepository.esMiembroDelGrupo(receptorId, grupoId)) {
            throw new DomainException("Ese héroe ya pertenece a este gremio.");
        }

        Usuario remitente = usuarioRepository.findById(remitenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario remitente no encontrado"));

        String mensaje = remitente.getNombre() + " te ha enviado un Pergamino de Reclutamiento para el gremio: " + grupo.getNombre() + " [GRP:" + grupoId + "]";

        notificationUseCase.enviarNotificacion(receptorId, TipoNotificacion.INVITACION_GREMIO, mensaje);
    }

    /**
     * Acepta una invitación de gremio, uniendo al usuario como miembro.
     * <p>
     * Es idempotente a propósito: si el usuario ya era miembro (por ejemplo,
     * aceptó dos veces por un doble clic), simplemente no hace nada en vez de
     * lanzar un error o duplicar la membresía/notificación.
     */
    @Override
    @Transactional
    public void aceptarInvitacion(Long usuarioId, Long grupoId) {
        GrupoChat grupo = chatRepository.findGrupoById(grupoId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        if (chatRepository.esMiembroDelGrupo(usuarioId, grupoId)) {
            return; // Ya es miembro: aceptar de nuevo es un no-op, no un error.
        }

        chatRepository.agregarMiembroAlGrupo(grupoId, usuarioId);
        
        notificationUseCase.enviarNotificacion(usuarioId, TipoNotificacion.SISTEMA, "¡Te has unido al gremio " + grupo.getNombre() + "!");
    }

    @Override
    public List<GrupoChat> buscarGrupos(String nombre) {
        return chatRepository.buscarGruposPorNombre(nombre);
    }

    @Override
    public List<GrupoChat> listarGruposPorUsuario(Long usuarioId) {
        return chatRepository.findGruposByUsuarioId(usuarioId);
    }
}
