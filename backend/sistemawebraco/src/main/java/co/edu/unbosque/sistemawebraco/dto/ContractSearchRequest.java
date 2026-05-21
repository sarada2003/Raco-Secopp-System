package co.edu.unbosque.sistemawebraco.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Parámetros de búsqueda y filtrado de contratos en SECOP II")
public class ContractSearchRequest {

    @Schema(description = "Búsqueda full-text en todos los campos del dataset ($q)", example = "consultoría tecnología")
    private String palabraClave;

    @Schema(description = "Búsqueda parcial por nombre de entidad (UPPER LIKE)", example = "Ministerio")
    private String entidad;

    @Schema(description = "Departamento exacto", example = "Cundinamarca")
    private String departamento;

    @Schema(description = "Modalidad de contratación exacta",
            example = "Licitación Pública",
            allowableValues = {"Licitación Pública", "Selección Abreviada de Menor Cuantía",
                    "Contratación directa", "Concurso de Méritos", "Mínima Cuantía",
                    "Contratación régimen especial"})
    private String modalidad;

    @Schema(description = "Estado del contrato",
            example = "activo",
            allowableValues = {"activo", "terminado", "liquidado", "cancelado", "en aprobación"})
    private String estado;

    @Schema(description = "Tipo de contrato",
            example = "Prestación de servicios",
            allowableValues = {"Obra", "Prestación de servicios", "Compraventa",
                    "Consultoría", "Suministro", "Arrendamiento", "Otro"})
    private String tipoContrato;

    @Schema(description = "Valor mínimo del contrato en COP", example = "50000000")
    private BigDecimal valorMin;

    @Schema(description = "Valor máximo del contrato en COP", example = "500000000")
    private BigDecimal valorMax;

    @Schema(description = "Fecha de firma desde (ISO 8601)", example = "2024-01-01")
    private LocalDate fechaFirmaDesde;

    @Schema(description = "Fecha de firma hasta (ISO 8601)", example = "2024-12-31")
    private LocalDate fechaFirmaHasta;

    @Schema(description = "Criterio de ordenamiento",
            example = "fecha_reciente",
            allowableValues = {"fecha_reciente", "fecha_antigua", "valor_mayor", "valor_menor"})
    private String ordenarPor;

    @Schema(description = "Página (base 0)", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Resultados por página", example = "20", defaultValue = "20")
    private Integer size = 20;
}
