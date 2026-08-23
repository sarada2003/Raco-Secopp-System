package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.entity.Notificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Envío del correo de alerta cuando el scheduler detecta contratos nuevos.
 *
 * <p>Se ejecuta con {@code @Async} para no bloquear al {@link NotificacionScheduler} mientras
 * espera al servidor SMTP. Las notificaciones ya quedaron guardadas en base de datos ANTES de
 * llamar a este método — si el correo falla (SMTP caído, credenciales inválidas, timeout), el
 * usuario de todas formas las ve en la interfaz. Por eso todo el cuerpo va en un try/catch que
 * solo loguea el error, nunca lo relanza.</p>
 *
 * <p>Todas las notificaciones nuevas de una misma corrida del scheduler para una misma
 * suscripción se agrupan en <b>un solo correo</b> — evita que una alerta con filtros amplios
 * (que puede matchear muchos contratos en su primera corrida) inunde el buzón del usuario con
 * un correo por contrato.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionMailService {

    private final JavaMailSender mailSender;

    @Value("${notificaciones.mail.from}")
    private String remitente;

    @Async
    public void enviarResumenAsync(String destinatario, List<Notificacion> notificaciones) {
        try {
            if (destinatario == null || destinatario.isBlank() || notificaciones.isEmpty()) {
                log.warn("No se envió correo de resumen: destinatario vacío o sin notificaciones nuevas");
                return;
            }

            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(notificaciones.size() == 1
                    ? "RACO — Nuevo contrato publicado en SECOP II"
                    : "RACO — " + notificaciones.size() + " nuevos contratos publicados en SECOP II");
            mensaje.setText(construirCuerpoResumen(notificaciones));

            mailSender.send(mensaje);
            log.info("Correo de resumen enviado a {} ({} notificación(es))", destinatario, notificaciones.size());
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de resumen a {} ({} notificación(es)): {}",
                    destinatario, notificaciones.size(), e.getMessage(), e);
        }
    }

    private String construirCuerpoResumen(List<Notificacion> notificaciones) {
        StringBuilder sb = new StringBuilder();
        sb.append("Se publicaron ").append(notificaciones.size())
                .append(notificaciones.size() == 1 ? " contrato nuevo" : " contratos nuevos")
                .append(" que coinciden con una de tus alertas en RACO.\n\n");

        int i = 1;
        for (Notificacion n : notificaciones) {
            sb.append(i++).append(". ").append(n.getNombreContrato() != null ? n.getNombreContrato() : "—").append("\n");
            sb.append("   Entidad: ").append(n.getEntidadContrato() != null ? n.getEntidadContrato() : "—").append("\n");
            if (n.getValorContrato() != null) {
                sb.append("   Valor: $").append(n.getValorContrato().toPlainString()).append("\n");
            }
            if (n.getUrlProceso() != null) {
                sb.append("   Ver proceso: ").append(n.getUrlProceso()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Puedes ver todas tus notificaciones dentro de tu cuenta en RACO.");
        return sb.toString();
    }
}
