package co.edu.unbosque.sistemawebraco.repository;

import co.edu.unbosque.sistemawebraco.entity.SuscripcionAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuscripcionAlertaRepository extends JpaRepository<SuscripcionAlerta, Long> {

    /**
     * Suscripciones activas con el usuario ya cargado (JOIN FETCH), para que el
     * scheduler pueda leer {@code usuario.getEmail()} sin depender de una sesión
     * de Hibernate abierta ni de marcar la relación como EAGER globalmente.
     */
    @Query("SELECT s FROM SuscripcionAlerta s JOIN FETCH s.usuario WHERE s.activo = true")
    List<SuscripcionAlerta> findActivasConUsuario();

    List<SuscripcionAlerta> findByUsuarioIdAndActivoTrueOrderByCreadoEnDesc(Long usuarioId);

    /** Usado para evitar crear dos suscripciones activas con exactamente el mismo criterio. */
    Optional<SuscripcionAlerta> findByUsuarioIdAndCriterioAndActivoTrue(Long usuarioId, String criterio);

    /** Verificación de dueño en la propia consulta, igual que en NotificacionRepository. */
    Optional<SuscripcionAlerta> findByIdAndUsuarioId(Long id, Long usuarioId);
}
