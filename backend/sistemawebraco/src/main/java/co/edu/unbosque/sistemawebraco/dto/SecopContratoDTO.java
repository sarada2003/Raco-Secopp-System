package co.edu.unbosque.sistemawebraco.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Proceso de contratación obtenido desde SECOP II (datos.gov.co — dataset p6dx-8zbt)")
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

    @JsonProperty("modalidad_de_contratacion")
    private String modalidad;

    @JsonProperty("estado_del_procedimiento")
    private String estado;

    @JsonProperty("tipo_de_contrato")
    private String tipoContrato;

    @JsonProperty("valor_total_adjudicacion")
    private BigDecimal valor;

    @JsonProperty("fecha_de_publicacion_del")
    private String fechaPublicacion;

    @JsonProperty("fecha_adjudicacion")
    private String fechaAdjudicacion;

    @JsonProperty("nombre_del_proveedor")
    private String proveedor;

    @JsonProperty("nit_del_proveedor_adjudicado")
    private String nitProveedor;

    @JsonProperty("urlproceso")
    @JsonDeserialize(using = UrlProcesoDeserializer.class)
    private String urlProceso;
}
