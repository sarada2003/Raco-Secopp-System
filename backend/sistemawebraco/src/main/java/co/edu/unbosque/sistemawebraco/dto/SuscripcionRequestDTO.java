package co.edu.unbosque.sistemawebraco.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Petición para crear una alerta recurrente a partir de un criterio de búsqueda de contratos")
public class SuscripcionRequestDTO {

    @Schema(description = "Mismos filtros usados en /api/secop/buscar. fechaFirmaDesde/Hasta y ordenarPor se ignoran " +
            "si vienen incluidos: el scheduler recalcula el rango de fechas en cada corrida.")
    private ContractSearchRequest criterio;

    @Schema(description = "Frecuencia de notificación", example = "diario", allowableValues = {"diario", "semanal"})
    private String frecuencia;
}
