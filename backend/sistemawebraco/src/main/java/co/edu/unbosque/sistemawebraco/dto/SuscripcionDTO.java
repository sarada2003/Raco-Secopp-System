package co.edu.unbosque.sistemawebraco.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Suscripción (alerta recurrente) configurada por el usuario, con el criterio ya deserializado para mostrarlo en la interfaz")
public class SuscripcionDTO {

    private Long id;
    private ContractSearchRequest criterio;
    private String frecuencia;
    private LocalDateTime ultimaEjecucion;
    private LocalDateTime creadoEn;
}
