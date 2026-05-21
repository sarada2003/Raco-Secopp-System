package co.edu.unbosque.sistemawebraco.dto;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.math.BigDecimal;

/**
 * Parámetros que GPT-4o-mini extrae del lenguaje natural del usuario
 * para consultar contratos en SECOP II.
 *
 * <p>Todos los campos son opcionales. Si el usuario no especifica algún filtro,
 * el campo llega null y la consulta se hace sin ese filtro.</p>
 */
@JsonClassDescription("Parámetros opcionales para filtrar contratos en SECOP II. Todos los campos son opcionales.")
public record SecopSearchInput(

        @JsonProperty(required = false)
        @JsonPropertyDescription("Palabra clave para buscar en la descripción del contrato. Ejemplo: 'consultoría', 'obras civiles', 'tecnología'")
        String palabraClave,

        @JsonProperty(required = false)
        @JsonPropertyDescription("Nombre o fragmento del nombre de la entidad pública contratante. Ejemplo: 'Ministerio', 'Alcaldía de Bogotá'")
        String entidad,

        @JsonProperty(required = false)
        @JsonPropertyDescription("Valor mínimo del contrato en pesos colombianos (COP). Ejemplo: 50000000 para cincuenta millones")
        BigDecimal valorMin
) {}
