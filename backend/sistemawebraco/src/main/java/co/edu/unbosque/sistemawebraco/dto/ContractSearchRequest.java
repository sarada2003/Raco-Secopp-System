package co.edu.unbosque.sistemawebraco.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractSearchRequest {

    private String palabraClave;
    private String entidad;
    private String modalidad;

    private BigDecimal valorMin;

    private Integer page = 0;
    private Integer size = 5;
}