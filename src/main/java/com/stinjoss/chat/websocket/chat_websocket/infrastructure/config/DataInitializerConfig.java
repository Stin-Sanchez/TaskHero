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
            nivelRepository.save(NivelEntity.builder().numeroNivel(2).xpRequerido(50).funcionDesbloqueada("Más misiones").descripcion("Aprendiz").build());
            nivelRepository.save(NivelEntity.builder().numeroNivel(3).xpRequerido(150).funcionDesbloqueada("Categorías").descripcion("Héroe Novato").build());
            nivelRepository.save(NivelEntity.builder().numeroNivel(4).xpRequerido(300).funcionDesbloqueada("Prioridades").descripcion("Guerrero").build());
            nivelRepository.save(NivelEntity.builder().numeroNivel(5).xpRequerido(500).xpRequerido(500).funcionDesbloqueada("Chat del Gremio").descripcion("Comandante").build());
            nivelRepository.save(NivelEntity.builder().numeroNivel(10).xpRequerido(2000).funcionDesbloqueada("Estadísticas").descripcion("Leyenda").build());
            System.out.println(">> Niveles de gamificación inicializados en la base de datos.");
        }
    }
}
