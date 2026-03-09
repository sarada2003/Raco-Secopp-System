package co.edu.unbosque.sistemawebraco.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SecopContratoDTO {

    @JsonProperty("id_del_proceso")
    private String idProceso;

    @JsonProperty("entidad")
    private String entidad;

    @JsonProperty("departamento_entidad")
    private String departamento;

    @JsonProperty("ciudad_entidad")
    private String ciudad;

    @JsonProperty("nombre_del_procedimiento")
    private String nombreProcedimiento;

    @JsonProperty("descripci_n_del_procedimiento")
    private String descripcion;

    @JsonProperty("valor_total_adjudicacion")
    private BigDecimal valor;

    @JsonProperty("fecha_de_publicacion_del")
    private String fechaPublicacion;
}