package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.InformeDetalleDTO;
import co.edu.unbosque.sistemawebraco.dto.InformeResumenDTO;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.entity.Informe;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.InformeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registra y consulta el historial de búsquedas de contratos SECOP II, que a su vez
 * es el módulo de informes (Anexo 11: "el informe no es fijo, sino ligado al historial
 * de consultas"). No existe una acción manual de "generar informe" — cada búsqueda real
 * contra SECOP II (filtro manual o tool-call del chatbot) se registra sola vía
 * {@link #registrarBusqueda}, y esta clase solo expone lectura/agregación sobre eso.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InformeService {

    private static final String ORIGEN_MANUAL = "MANUAL";
    private static final String ORIGEN_CHATBOT = "CHATBOT";

    private final InformeRepository informeRepository;
    private final ObjectMapper objectMapper;

    /**
     * Guarda el snapshot de una búsqueda ya ejecutada contra SECOP II. Nunca propaga
     * una excepción: un fallo al guardar el historial no debe tumbar la respuesta de
     * búsqueda (manual o chatbot) que ya se le va a devolver al usuario.
     */
    public void registrarBusqueda(Usuario usuario, ContractSearchRequest criterio,
                                   List<SecopContratoDTO> resultados, String origen) {
        if (usuario == null) {
            log.warn("Búsqueda origen={} sin usuario asociado; no se guarda en el historial", origen);
            return;
        }
        try {
            Informe informe = new Informe();
            informe.setUsuario(usuario);
            informe.setOrigen(origen);
            informe.setNombre(nombreDesdeCriterio(criterio));
            informe.setCantidadResultados(resultados != null ? resultados.size() : 0);
            informe.setFecha(LocalDateTime.now());
            informe.setCriterios(objectMapper.writeValueAsString(criterio));
            informe.setResultados(objectMapper.writeValueAsString(resultados != null ? resultados : List.of()));
            informeRepository.save(informe);
        } catch (Exception e) {
            log.error("No se pudo guardar la búsqueda en el historial (usuario={}, origen={}): {}",
                    usuario.getId(), origen, e.getMessage());
        }
    }

    public void registrarBusquedaManual(Usuario usuario, ContractSearchRequest criterio, List<SecopContratoDTO> resultados) {
        registrarBusqueda(usuario, criterio, resultados, ORIGEN_MANUAL);
    }

    public void registrarBusquedaChatbot(Usuario usuario, ContractSearchRequest criterio, List<SecopContratoDTO> resultados) {
        registrarBusqueda(usuario, criterio, resultados, ORIGEN_CHATBOT);
    }

    public List<InformeResumenDTO> listar(Long usuarioId) {
        return informeRepository.findByUsuarioIdOrderByFechaDesc(usuarioId)
                .stream()
                .map(this::toResumenDTO)
                .toList();
    }

    /** Verificación de dueño en la propia consulta: un id de otro usuario responde 404, igual que no existiese. */
    public InformeDetalleDTO obtener(Long informeId, Long usuarioId) {
        Informe informe = informeRepository.findByIdAndUsuarioId(informeId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Informe no encontrado"));

        InformeDetalleDTO dto = toResumenComoDetalle(informe);
        try {
            dto.setCriterio(objectMapper.readValue(informe.getCriterios(), ContractSearchRequest.class));
            dto.setResultados(informe.getResultados() != null
                    ? objectMapper.readValue(informe.getResultados(), new TypeReference<List<SecopContratoDTO>>() {})
                    : List.of());
        } catch (IOException e) {
            log.error("No se pudo deserializar el informe id={}: {}", informe.getId(), e.getMessage());
            dto.setCriterio(new ContractSearchRequest());
            dto.setResultados(List.of());
        }
        return dto;
    }

    /** Etiqueta legible para el historial ya que nadie escribe un título a mano. */
    private String nombreDesdeCriterio(ContractSearchRequest c) {
        List<String> partes = new ArrayList<>();
        if (esNoVacio(c.getPalabraClave())) partes.add("\"" + c.getPalabraClave().trim() + "\"");
        if (esNoVacio(c.getEntidad())) partes.add(c.getEntidad().trim());
        if (esNoVacio(c.getDepartamento())) partes.add(c.getDepartamento());
        if (esNoVacio(c.getModalidad())) partes.add(c.getModalidad());
        if (esNoVacio(c.getTipoContrato())) partes.add(c.getTipoContrato());

        if (partes.isEmpty()) return "Búsqueda general - " + LocalDate.now();
        return "Búsqueda: " + String.join(", ", partes);
    }

    private boolean esNoVacio(String s) {
        return s != null && !s.isBlank();
    }

    private InformeResumenDTO toResumenDTO(Informe i) {
        InformeResumenDTO dto = new InformeResumenDTO();
        dto.setId(i.getId());
        dto.setNombre(i.getNombre());
        dto.setFecha(i.getFecha());
        dto.setCantidadResultados(i.getCantidadResultados());
        dto.setOrigen(i.getOrigen());
        return dto;
    }

    private InformeDetalleDTO toResumenComoDetalle(Informe i) {
        InformeDetalleDTO dto = new InformeDetalleDTO();
        dto.setId(i.getId());
        dto.setNombre(i.getNombre());
        dto.setFecha(i.getFecha());
        dto.setCantidadResultados(i.getCantidadResultados());
        dto.setOrigen(i.getOrigen());
        return dto;
    }
}
