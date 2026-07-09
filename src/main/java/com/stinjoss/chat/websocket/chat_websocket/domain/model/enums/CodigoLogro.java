package com.stinjoss.chat.websocket.chat_websocket.domain.model.enums;

public enum CodigoLogro {
    PRIMERA_MISION("Primeros Pasos", "Completa tu primera misión", "bi-flag-fill"),
    VETERANO_TAREAS("Veterano", "Completa 25 misiones", "bi-shield-fill"),
    LEYENDA_TAREAS("Leyenda", "Completa 100 misiones", "bi-trophy-fill"),
    ESTRATEGA("Estratega", "Completa 10 misiones de prioridad Alta", "bi-bullseye"),
    DIA_PERFECTO("Día Perfecto", "Completa todas tus misiones del día", "bi-calendar-check-fill"),
    CRONOMETRISTA("Cronometrista", "Acumula 5 horas usando el cronómetro", "bi-stopwatch-fill"),
    RACHA_SEMANAL("Constancia de Hierro", "Mantén una racha de 7 días", "bi-fire"),
    RACHA_MENSUAL("Disciplina Absoluta", "Mantén una racha de 30 días", "bi-fire"),
    ASCENSO_COMANDANTE("Ascenso a Comandante", "Alcanza el Nivel 5", "bi-award-fill"),
    ASCENSO_MAESTRO("Ascenso a Maestro", "Alcanza el Nivel 8", "bi-award-fill");

    private final String nombre;
    private final String descripcion;
    private final String icono;

    CodigoLogro(String nombre, String descripcion, String icono) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.icono = icono;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getIcono() {
        return icono;
    }
}
