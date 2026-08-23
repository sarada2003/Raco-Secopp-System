package co.edu.unbosque.sistemawebraco.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Snapshot automático de cada búsqueda de contratos SECOP II ejecutada por un usuario,
 * ya sea desde el filtro manual o desde el chatbot. No se crea a demanda: una fila nueva
 * se guarda cada vez que {@code SecopService.buscarContratos} se invoca en nombre de un
 * usuario autenticado — el módulo de informes lee y agrega este historial en vez de
 * pedirle al usuario que "genere" un informe.
 *
 * <p>Mapea la tabla {@code historial_busquedas} ya documentada en el Anexo 11 en vez de crear
 * una tabla {@code informes} aparte: el documento describe el informe como algo "ligado al
 * historial de consultas", y esta tabla ya representa exactamente eso — cada fila es a la vez
 * el registro de "qué buscaste" y el propio artefacto del informe (criterio + resultados).</p>
 *
 * <p>{@code resultados} guarda un snapshot JSON de {@link co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO}
 * en el momento de generar el informe — no se vuelve a consultar SECOP II cada vez que el
 * usuario abre un informe viejo, así el PDF exportado siempre coincide con lo que vio al
 * generarlo, incluso si esos datos cambian más adelante en la fuente en vivo.</p>
 */
@Entity
@Table(name = "historial_busquedas")
@Data
@NoArgsConstructor
public class Informe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nombre")
    private String nombre;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criterios", columnDefinition = "jsonb", nullable = false)
    private String criterios;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resultados", columnDefinition = "jsonb")
    private String resultados;

    @Column(name = "cantidad_resultados")
    private Integer cantidadResultados;

    /** "MANUAL" (filtro de Contratos SECOP) o "CHATBOT" (tool-call del asistente IA). */
    @Column(name = "origen", nullable = false, length = 20, columnDefinition = "varchar(20) default 'MANUAL'")
    private String origen;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    protected void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
