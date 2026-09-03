package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.NotificacionDTO;
import co.edu.unbosque.sistemawebraco.dto.SuscripcionDTO;
import co.edu.unbosque.sistemawebraco.dto.SuscripcionRequestDTO;
import co.edu.unbosque.sistemawebraco.entity.Notificacion;
import co.edu.unbosque.sistemawebraco.entity.SuscripcionAlerta;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.NotificacionRepository;
import co.edu.unbosque.sistemawebraco.repository.SuscripcionAlertaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio del módulo de notificaciones: lectura/marcado de alertas ya generadas,
 * y alta de nuevas suscripciones (criterios recurrentes) que el {@link NotificacionScheduler}
 * evaluará periódicamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final SuscripcionAlertaRepository suscripcionRepository;
    private final ObjectMapper objectMapper;

    public List<NotificacionDTO> listar(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByLeidaAscCreadoEnDesc(usuarioId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    /**
     * Marca una notificación como leída, verificando que pertenezca al usuario autenticado.
     * La verificación de dueño ocurre en la propia consulta ({@code findByIdAndUsuarioId}):
     * si el id existe pero es de otro usuario, esto se comporta igual que un id inexistente.
     */
    public void marcarLeida(Long notificacionId, Long usuarioId) {
        Notificacion notificacion = notificacionRepository.findByIdAndUsuarioId(notificacionId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));

        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacionRepository.save(notificacion);
        }
    }

    public void marcarTodasLeidas(Long usuarioId) {
        notificacionRepository.marcarTodasLeidas(usuarioId);
    }

    /**
     * Crea una suscripción (alerta recurrente) a partir de un criterio de búsqueda.
     *
     * <p>Limpia los campos que no tienen sentido en una alerta recurrente
     * (rango de fechas, orden, página) porque el scheduler los recalcula en cada corrida.
     * Antes de insertar, verifica que el usuario no tenga ya una suscripción activa con
     * exactamente el mismo criterio — si la tiene, la devuelve tal cual en vez de duplicarla.</p>
     */
    public SuscripcionAlerta crearSuscripcion(Usuario usuario, SuscripcionRequestDTO request) {
        ContractSearchRequest criterio = request.getCriterio() != null ? request.getCriterio() : new ContractSearchRequest();
        criterio.setFechaFirmaDesde(null);
        criterio.setFechaFirmaHasta(null);
        criterio.setOrdenarPor(null);
        criterio.setPage(0);

        String criterioJson;
        try {
            criterioJson = objectMapper.writeValueAsString(criterio);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Criterio de búsqueda inválido");
        }

        var existente = suscripcionRepository.findByUsuarioIdAndCriterioAndActivoTrue(usuario.getId(), criterioJson);
        if (existente.isPresent()) {
            log.info("Suscripción duplicada evitada para usuario {}: ya existe la id={}", usuario.getId(), existente.get().getId());
            return existente.get();
        }

        String frecuencia = normalizarFrecuencia(request.getFrecuencia());

        SuscripcionAlerta suscripcion = new SuscripcionAlerta();
        suscripcion.setUsuario(usuario);
        suscripcion.setCriterio(criterioJson);
        suscripcion.setFrecuencia(frecuencia);
        suscripcion.setActivo(true);
        suscripcion.setCreadoEn(LocalDateTime.now());

        return suscripcionRepository.save(suscripcion);
    }

    private String normalizarFrecuencia(String frecuencia) {
        if ("semanal".equalsIgnoreCase(frecuencia)) return "semanal";
        return "diario";
    }

    public List<SuscripcionDTO> listarSuscripciones(Long usuarioId) {
        return suscripcionRepository.findByUsuarioIdAndActivoTrueOrderByCreadoEnDesc(usuarioId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /** Desactiva (soft delete) una suscripción, verificando que pertenezca al usuario autenticado. */
    public void eliminarSuscripcion(Long suscripcionId, Long usuarioId) {
        SuscripcionAlerta suscripcion = suscripcionRepository.findByIdAndUsuarioId(suscripcionId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suscripción no encontrada"));

        suscripcion.setActivo(false);
        suscripcionRepository.save(suscripcion);
    }

    private SuscripcionDTO toDTO(SuscripcionAlerta s) {
        SuscripcionDTO dto = new SuscripcionDTO();
        dto.setId(s.getId());
        dto.setFrecuencia(s.getFrecuencia());
        dto.setUltimaEjecucion(s.getUltimaEjecucion());
        dto.setCreadoEn(s.getCreadoEn());
        try {
            dto.setCriterio(objectMapper.readValue(s.getCriterio(), ContractSearchRequest.class));
        } catch (IOException e) {
            log.error("No se pudo deserializar el criterio de la suscripción id={}: {}", s.getId(), e.getMessage());
            dto.setCriterio(new ContractSearchRequest());
        }
        return dto;
    }

    private NotificacionDTO toDTO(Notificacion n) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(n.getId());
        dto.setIdProcesoContrato(n.getIdProcesoContrato());
        dto.setNombreContrato(n.getNombreContrato());
        dto.setEntidadContrato(n.getEntidadContrato());
        dto.setValorContrato(n.getValorContrato());
        dto.setFechaPublicacionContrato(n.getFechaPublicacionContrato());
        dto.setUrlProceso(n.getUrlProceso());
        dto.setCategoria(n.getCategoria());
        dto.setLeida(n.isLeida());
        dto.setCreadoEn(n.getCreadoEn());
        return dto;
    }
}
