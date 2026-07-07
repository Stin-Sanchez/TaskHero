package com.stinjoss.chat.websocket.chat_websocket.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    @DisplayName("Debe sumar XP y subir de nivel cuando se alcanza el umbral")
    void sumarXpYSubirNivel() {
        // Arrange
        Usuario usuario = Usuario.builder()
                .xpTotal(190)
                .nivelActual(1)
                .build();

        List<Nivel> niveles = List.of(
                new Nivel(1, 0, "Nivel 1", "Inicio"),
                new Nivel(2, 200, "Nivel 2", "Desbloqueo avanzado")
        );

        // Act
        boolean subio = usuario.sumarXP(10, niveles);

        // Assert
        assertTrue(subio, "El usuario debería haber subido de nivel");
        assertEquals(2, usuario.getNivelActual());
        assertEquals(200, usuario.getXpTotal());
    }

    @Test
    @DisplayName("Debe incrementar la racha si el último login fue ayer")
    void actualizarRachaConsecutiva() {
        Usuario usuario = Usuario.builder()
                .rachaDias(5)
                .ultimoLogin(java.time.LocalDateTime.now().minusDays(1))
                .build();

        usuario.actualizarRacha();

        assertEquals(6, usuario.getRachaDias(), "La racha debería haber incrementado a 6");
    }

    @Test
    @DisplayName("Debe resetear la racha a 1 si el último login fue hace más de un día")
    void resetearRachaSiPasaTiempo() {
        Usuario usuario = Usuario.builder()
                .rachaDias(10)
                .ultimoLogin(java.time.LocalDateTime.now().minusDays(3))
                .build();

        usuario.actualizarRacha();

        assertEquals(1, usuario.getRachaDias(), "La racha debería reiniciarse a 1");
    }

    @Test
    @DisplayName("No debe alterar la racha si el login es el mismo día")
    void noAlterarRachaMismoDia() {
        java.time.LocalDateTime hoy = java.time.LocalDateTime.now();
        Usuario usuario = Usuario.builder()
                .rachaDias(3)
                .ultimoLogin(hoy)
                .build();

        usuario.actualizarRacha();

        assertEquals(3, usuario.getRachaDias(), "La racha no debería cambiar si se loguea el mismo día");
    }

    @Test
    @DisplayName("Debe identificar si es el primer login del día correctamente")
    void identificarPrimerLoginDelDia() {
        Usuario usuarioAyer = Usuario.builder()
                .ultimoLogin(java.time.LocalDateTime.now().minusDays(1))
                .build();
        
        Usuario usuarioHoy = Usuario.builder()
                .ultimoLogin(java.time.LocalDateTime.now())
                .build();

        assertTrue(usuarioAyer.esPrimerLoginDelDia());
        assertFalse(usuarioHoy.esPrimerLoginDelDia());
    }
}
