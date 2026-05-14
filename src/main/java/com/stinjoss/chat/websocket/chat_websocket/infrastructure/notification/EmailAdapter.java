package com.stinjoss.chat.websocket.chat_websocket.infrastructure.notification;

import com.stinjoss.chat.websocket.chat_websocket.application.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    @Override
    @Async // Enviamos correos de forma asíncrona para no bloquear la ejecución del hilo principal
    public void enviarCorreoBienvenida(String destinatario, String nombreUsuario) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinatario);
        message.setSubject("¡Bienvenido a TaskHero, " + nombreUsuario + "!");
        message.setText("Tu aventura comienza ahora. Completa misiones, sube de nivel y desbloquea el chat del gremio.\n\n¡Mucha suerte, Héroe!");
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar email de bienvenida: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void enviarCorreoLogro(String destinatario, String mensaje) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinatario);
        message.setSubject("¡Nuevo Logro en TaskHero!");
        message.setText(mensaje);
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar email de logro: " + e.getMessage());
        }
    }
}
