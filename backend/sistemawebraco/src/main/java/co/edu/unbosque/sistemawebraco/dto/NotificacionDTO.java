package co.edu.unbosque.sistemawebraco.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Alerta de un contrato de SECOP II que coincidió con una suscripción del usuario")
public class NotificacionDTO {

    private Long id;
    private String idProcesoContrato;
    private String nombreContrato;
    private String entidadContrato;
    private BigDecimal valorContrato;
    private String fechaPublicacionContrato;
    private String urlProceso;
    private String categoria;
    private boolean leida;
    private LocalDateTime creadoEn;
}
