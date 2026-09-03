package co.edu.unbosque.sistemawebraco.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Detalle completo de un informe: criterio usado y snapshot de los contratos encontrados al generarlo")
public class InformeDetalleDTO {

    private Long id;
    private String nombre;
    private LocalDateTime fecha;
    private Integer cantidadResultados;
    private String origen;
    private ContractSearchRequest criterio;
    private List<SecopContratoDTO> resultados;
}
