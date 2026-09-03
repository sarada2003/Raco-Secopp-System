package co.edu.unbosque.sistemawebraco.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Alerta generada para un usuario cuando un contrato de SECOP II coincide con alguna
 * de sus {@link SuscripcionAlerta}.
 *
 * <p>Guarda una copia mínima ("snapshot") de los datos del contrato en el momento de la
 * detección, en vez de referenciar una tabla de contratos: el proyecto no persiste
 * contratos en ningún otro lado (SecopService consulta datos.gov.co en vivo y no guarda
 * nada localmente), así que crear una tabla de referencia aparte solo para esto sería
 * una capa extra sin otro consumidor.</p>
 *
 * <p>La restricción única {@code (usuario_id, id_proceso_contrato)} es el control de
 * duplicados: evita que una misma corrida del scheduler (o corridas repetidas) generen
 * más de una notificación del mismo contrato para el mismo usuario.</p>
 */
@Entity
@Table(
        name = "notificaciones",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notificacion_usuario_contrato",
                columnNames = {"usuario_id", "id_proceso_contrato"}
        )
)
@Data
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "id_proceso_contrato", nullable = false)
    private String idProcesoContrato;

    @Column(name = "nombre_contrato")
    private String nombreContrato;

    @Column(name = "entidad_contrato")
    private String entidadContrato;

    @Column(name = "valor_contrato")
    private BigDecimal valorContrato;

    @Column(name = "fecha_publicacion_contrato")
    private String fechaPublicacionContrato;

    @Column(name = "url_proceso")
    private String urlProceso;

    /** "contrato" | "alerta" | "sistema" — coincide con las pestañas del frontend. */
    @Column(name = "categoria", nullable = false, length = 30)
    private String categoria = "contrato";

    @Column(name = "leida", nullable = false)
    private boolean leida = false;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void alCrear() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }
}
