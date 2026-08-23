package co.edu.unbosque.sistemawebraco.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Suscripción de un usuario a un criterio de búsqueda recurrente sobre SECOP II.
 *
 * <p>Cada fila representa "avísame cuando aparezca un contrato que cumpla estos filtros,
 * con esta frecuencia". El {@link co.edu.unbosque.sistemawebraco.services.NotificacionScheduler}
 * recorre las suscripciones activas, respeta {@code frecuencia} comparando contra
 * {@code ultimaEjecucion}, y genera {@link Notificacion} por cada contrato nuevo que coincida.</p>
 *
 * <p>{@code criterio} guarda el JSON serializado de un
 * {@link co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest} — se reutiliza el mismo
 * DTO que ya usa la búsqueda manual de contratos, en vez de duplicar el modelo de filtros.</p>
 */
@Entity
@Table(name = "notificaciones_programadas")
@Data
@NoArgsConstructor
public class SuscripcionAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criterio", columnDefinition = "jsonb", nullable = false)
    private String criterio;

    /** "diario" o "semanal". Cualquier otro valor se trata como "diario". */
    @Column(name = "frecuencia", nullable = false, length = 50)
    private String frecuencia;

    @Column(name = "ultima_ejecucion")
    private LocalDateTime ultimaEjecucion;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void alCrear() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }
}
