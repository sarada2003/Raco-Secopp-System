package co.edu.unbosque.sistemawebraco.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Resumen de un informe generado (para el listado, sin el snapshot completo de resultados)")
public class InformeResumenDTO {

    private Long id;
    private String nombre;
    private LocalDateTime fecha;
    private Integer cantidadResultados;

    @Schema(description = "Origen de la búsqueda: MANUAL (filtro de Contratos SECOP) o CHATBOT", example = "MANUAL")
    private String origen;
}
