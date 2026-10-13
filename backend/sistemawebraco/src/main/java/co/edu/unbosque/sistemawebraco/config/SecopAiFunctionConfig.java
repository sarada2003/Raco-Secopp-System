package co.edu.unbosque.sistemawebraco.config;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.dto.SecopSearchInput;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.services.ChatbotService;
import co.edu.unbosque.sistemawebraco.services.InformeService;
import co.edu.unbosque.sistemawebraco.services.SecopService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.function.BiFunction;

@Configuration
public class SecopAiFunctionConfig {

    @Bean
    @Description("Busca contratos públicos reales publicados en SECOP II (Colombia Compra Eficiente). " +
                 "Úsala cuando el usuario quiera buscar, consultar, encontrar o listar contratos del Estado colombiano. " +
                 "Todos los parámetros son opcionales: si el usuario no especifica alguno, omítelo.")
    public BiFunction<SecopSearchInput, ToolContext, String> buscarContratosSecop(SecopService secopService,
                                                                               InformeService informeService,
                                                                               MeterRegistry meterRegistry) {
        return (input, toolContext) -> {
            Timer.Sample muestra = Timer.start(meterRegistry);
            String resultado = "ok";
            try {
                ContractSearchRequest req = new ContractSearchRequest();
                req.setPalabraClave(input.palabraClave());
                req.setEntidad(input.entidad());
                req.setValorMin(input.valorMin());
                req.setPage(0);
                req.setSize(5);

                List<SecopContratoDTO> contratos;
                try {
                    contratos = secopService.buscarContratos(req);
                    // Asíncrono (@Async): guardar el historial no retrasa la respuesta del modelo.
                    informeService.registrarBusquedaChatbot(usuarioDe(toolContext), req, contratos);
                } catch (ResponseStatusException rse) {
                    resultado = "timeout";
                    return "No pude consultar SECOP II en este momento (la API tardó demasiado). " +
                           "Puedo responder preguntas generales sobre contratación pública.";
                }

                if (contratos == null || contratos.isEmpty()) {
                    resultado = "sin_resultados";
                    return "No se encontraron contratos con los filtros indicados en SECOP II.";
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Contratos encontrados en SECOP II: ").append(contratos.size()).append("\n\n");

                for (int i = 0; i < contratos.size(); i++) {
                    SecopContratoDTO c = contratos.get(i);
                    sb.append(i + 1).append(". ")
                      .append(c.getNombreProcedimiento() != null ? c.getNombreProcedimiento() : "Sin nombre")
                      .append("\n");

                    if (c.getIdProceso() != null)
                        sb.append("   ID: ").append(c.getIdProceso()).append("\n");

                    if (c.getEntidad() != null)
                        sb.append("   Entidad: ").append(c.getEntidad()).append("\n");

                    if (c.getDepartamento() != null || c.getCiudad() != null) {
                        sb.append("   Ubicación: ");
                        if (c.getCiudad() != null)        sb.append(c.getCiudad());
                        if (c.getDepartamento() != null)  sb.append(", ").append(c.getDepartamento());
                        sb.append("\n");
                    }

                    if (c.getModalidad() != null)
                        sb.append("   Modalidad: ").append(c.getModalidad()).append("\n");

                    if (c.getEstado() != null)
                        sb.append("   Estado: ").append(c.getEstado()).append("\n");

                    // SECOP reporta 0 en valor_total_adjudicacion mientras el proceso no se ha adjudicado
                    if (c.getValor() != null && c.getValor().signum() > 0)
                        sb.append("   Valor: $").append(c.getValor().toPlainString()).append(" COP\n");
                    else
                        sb.append("   Valor: Sin adjudicar\n");

                    if (c.getFechaPublicacion() != null && c.getFechaPublicacion().length() >= 10)
                        sb.append("   Publicado: ").append(c.getFechaPublicacion(), 0, 10).append("\n");

                    if (c.getProveedor() != null)
                        sb.append("   Proveedor: ").append(c.getProveedor()).append("\n");

                    if (c.getUrlProceso() != null && !c.getUrlProceso().isBlank())
                        sb.append("   Enlace: ").append(c.getUrlProceso()).append("\n");

                    sb.append("\n");
                }

                return sb.toString();

            } catch (Exception e) {
                resultado = "error";
                return "Error al consultar SECOP II: " + e.getMessage() +
                       ". Verifica que el servidor tenga conectividad con datos.gov.co.";
            } finally {
                muestra.stop(Timer.builder("chatbot.herramienta.secop")
                        .description("Tiempo de la herramienta buscarContratosSecop (consulta a SECOP II)")
                        .tag("resultado", resultado)
                        .register(meterRegistry));
            }
        };
    }

    private static Usuario usuarioDe(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) return null;
        return (Usuario) toolContext.getContext().get(ChatbotService.TOOL_CONTEXT_USUARIO);
    }
}
