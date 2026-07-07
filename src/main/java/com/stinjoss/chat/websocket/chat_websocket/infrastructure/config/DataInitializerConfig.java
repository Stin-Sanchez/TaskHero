package com.stinjoss.chat.websocket.chat_websocket.infrastructure.config;

import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.entity.NivelEntity;
import com.stinjoss.chat.websocket.chat_websocket.infrastructure.persistence.repository.JpaNivelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializerConfig implements CommandLineRunner {

    private final JpaNivelRepository nivelRepository;

    @Override
    public void run(String... args) {
        if (nivelRepository.count() == 0) {
            nivelRepository.save(NivelEntity.builder().numeroNivel(1).xpRequerido(0).funcionDesbloqueada("Funciones básicas").descripcion("Nivel inicial").build());
            nivelRepository.save(NivelEntity.builder().numeroNivel(3).xpRequerido(200).funcionDesbloqueada("Categorías y Prioridades Avanzadas").descripcion("Héroe Novato").build());
            nivelRepository.save(NivelEntity.builder().numeroNivel(5).xpRequerido(500).funcionDesbloqueada("Chat entre amigos habilitado").descripcion("Comandante").build());
            nivelRepository.save(NivelEntity.builder().numeroNivel(8).xpRequerido(1000).funcionDesbloqueada("Grupos de estudio y colaboración").descripcion("Maestro").build());
            nivelRepository.save(NivelEntity.builder().numeroNivel(10).xpRequerido(2000).funcionDesbloqueada("Estadísticas y exportación").descripcion("Leyenda").build());
            System.out.println(">> Niveles de gamificación sincronizados con el Anteproyecto.");
        }
    }
}
