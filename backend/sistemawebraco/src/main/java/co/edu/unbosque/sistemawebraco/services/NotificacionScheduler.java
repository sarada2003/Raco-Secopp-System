package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.entity.Notificacion;
import co.edu.unbosque.sistemawebraco.entity.SuscripcionAlerta;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.NotificacionRepository;
import co.edu.unbosque.sistemawebraco.repository.SuscripcionAlertaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Disparador periódico del módulo de notificaciones.
 *
 * <p>SECOP II no ofrece webhooks, así que la única forma de detectar contratos nuevos es
 * preguntar periódicamente. El {@code cron} de {@code @Scheduled} (configurable en
 * {@code notificaciones.scheduler.cron}, cada hora por defecto) es solo el "reloj" del
 * sistema — la <b>granularidad mínima</b> posible. La frecuencia real que percibe cada
 * usuario ("diario"/"semanal") la decide {@link #debeEjecutarse}, comparando
 * {@code frecuencia} contra {@code ultimaEjecucion} de su suscripción. Con un tick de 1h,
 * "diario" y "semanal" funcionan bien; algo más fino que el tick no sería posible.</p>
 *
 * <p>Reutiliza {@link SecopService#buscarContratos(ContractSearchRequest)} — el mismo método
 * que usa la búsqueda manual del frontend — en vez de construir una consulta SOQL aparte.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionScheduler {

    private final SuscripcionAlertaRepository suscripcionRepository;
    private final NotificacionRepository notificacionRepository;
    private final SecopService secopService;
    private final NotificacionMailService mailService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${notificaciones.scheduler.cron}")
    public void revisarSuscripciones() {
        List<SuscripcionAlerta> activas = suscripcionRepository.findActivasConUsuario();
        LocalDateTime ahora = LocalDateTime.now();

        log.info("NotificacionScheduler: revisando {} suscripción(es) activa(s)", activas.size());

        for (SuscripcionAlerta suscripcion : activas) {
            try {
                if (debeEjecutarse(suscripcion, ahora)) {
                    procesarSuscripcion(suscripcion, ahora);
                }
            } catch (Exception e) {
                // Un fallo en una suscripción (ej. SECOP no responde) no debe tumbar el resto del batch.
                log.error("Error procesando suscripción id={}: {}", suscripcion.getId(), e.getMessage(), e);
            }
        }
    }

    private boolean debeEjecutarse(SuscripcionAlerta suscripcion, LocalDateTime ahora) {
        if (suscripcion.getUltimaEjecucion() == null) return true;
        long diasMinimos = "semanal".equalsIgnoreCase(suscripcion.getFrecuencia()) ? 7 : 1;
        return suscripcion.getUltimaEjecucion().isBefore(ahora.minusDays(diasMinimos));
    }

    private void procesarSuscripcion(SuscripcionAlerta suscripcion, LocalDateTime ahora) throws Exception {
        Usuario usuario = suscripcion.getUsuario();

        ContractSearchRequest criterio = objectMapper.readValue(suscripcion.getCriterio(), ContractSearchRequest.class);
        criterio.setFechaFirmaDesde(fechaDesde(suscripcion, ahora));
        criterio.setFechaFirmaHasta(null);
        criterio.setOrdenarPor("fecha_reciente");
        criterio.setPage(0);
        if (criterio.getSize() == null) criterio.setSize(50);

        log.info("Suscripción id={}: frecuencia='{}', ultimaEjecucion={}, buscando desde={}",
                suscripcion.getId(), suscripcion.getFrecuencia(), suscripcion.getUltimaEjecucion(), criterio.getFechaFirmaDesde());

        List<SecopContratoDTO> contratos = secopService.buscarContratos(criterio);

        List<Notificacion> nuevas = new ArrayList<>();
        for (SecopContratoDTO contrato : contratos) {
            if (contrato.getIdProceso() == null) continue;
            if (notificacionRepository.existsByUsuarioIdAndIdProcesoContrato(usuario.getId(), contrato.getIdProceso())) {
                continue; // ya se le notificó este contrato antes — evita duplicados
            }

            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(usuario);
            notificacion.setIdProcesoContrato(contrato.getIdProceso());
            notificacion.setNombreContrato(contrato.getNombreProcedimiento());
            notificacion.setEntidadContrato(contrato.getEntidad());
            notificacion.setValorContrato(contrato.getValor());
            notificacion.setFechaPublicacionContrato(contrato.getFechaPublicacion());
            notificacion.setUrlProceso(contrato.getUrlProceso());
            notificacion.setCategoria("contrato");
            notificacion.setLeida(false);
            notificacion.setCreadoEn(LocalDateTime.now());

            notificacionRepository.save(notificacion);
            nuevas.add(notificacion);
        }

        suscripcion.setUltimaEjecucion(ahora);
        suscripcionRepository.save(suscripcion);

        // Todas las notificaciones de esta corrida ya están persistidas: si el correo
        // resumen falla, siguen visibles igual en la app. Un solo correo por corrida
        // (no uno por contrato) evita que una alerta amplia inunde el buzón del usuario.
        if (!nuevas.isEmpty()) {
            mailService.enviarResumenAsync(usuario.getEmail(), nuevas);
        }

        log.info("Suscripción id={} (usuario={}): {} notificación(es) nueva(s)",
                suscripcion.getId(), usuario.getEmail(), nuevas.size());
    }

    private LocalDate fechaDesde(SuscripcionAlerta suscripcion, LocalDateTime ahora) {
        if (suscripcion.getUltimaEjecucion() != null) {
            return suscripcion.getUltimaEjecucion().toLocalDate();
        }
        long diasIniciales = "semanal".equalsIgnoreCase(suscripcion.getFrecuencia()) ? 7 : 1;
        return ahora.toLocalDate().minusDays(diasIniciales);
    }
}
