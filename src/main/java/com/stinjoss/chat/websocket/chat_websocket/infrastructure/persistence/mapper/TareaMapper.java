package com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.mapper;

import com.stinjoss.chat.websocket.chat_websocket.domain.model.Tarea;
import com.stinjoss.chat.websocket.chat_websocket.domain.model.enums.Prioridad;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.TareaEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.UsuarioEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.enums.PrioridadTarea;
import org.springframework.stereotype.Component;

@Component
public class TareaMapper {

    public Tarea toDomain(TareaEntity entity) {
        if (entity == null) return null;

        return Tarea.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuario().getId())
                .titulo(entity.getTitulo())
                .descripcion(entity.getDescripcion())
                .prioridad(Prioridad.valueOf(entity.getPrioridad().name()))
                .fechaLimite(entity.getFechaLimite())
                .completada(entity.getCompletada())
                .categoria(entity.getCategoria())
                .build();
    }

    public TareaEntity toEntity(Tarea domain, UsuarioEntity usuarioEntity) {
        if (domain == null) return null;

        return TareaEntity.builder()
                .id(domain.getId())
                .usuario(usuarioEntity)
                .titulo(domain.getTitulo())
                .descripcion(domain.getDescripcion())
                .prioridad(PrioridadTarea.valueOf(domain.getPrioridad().name()))
                .fechaLimite(domain.getFechaLimite())
                .completada(domain.isCompletada())
                .categoria(domain.getCategoria())
                .build();
    }
}
