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
    @DisplayName("No debe subir de nivel si no alcanza el XP requerido")
    void noDebeSubirNivelSiNoAlcanzaXP() {
        Usuario usuario = Usuario.builder().xpTotal(0).nivelActual(1).build();
        List<Nivel> niveles = List.of(new Nivel(2, 200, "N2", "Desc"));

        boolean subio = usuario.sumarXP(100, niveles);

        assertFalse(subio);
        assertEquals(1, usuario.getNivelActual());
    }
}
