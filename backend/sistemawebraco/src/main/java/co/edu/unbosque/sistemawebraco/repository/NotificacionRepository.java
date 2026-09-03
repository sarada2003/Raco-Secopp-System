package co.edu.unbosque.sistemawebraco.repository;

import co.edu.unbosque.sistemawebraco.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /** No leídas primero, más recientes primero dentro de cada grupo. */
    List<Notificacion> findByUsuarioIdOrderByLeidaAscCreadoEnDesc(Long usuarioId);

    /**
     * Carga por id filtrando también por dueño: la verificación de propiedad ocurre
     * en la propia consulta, no como un chequeo posterior en memoria. Un id que existe
     * pero pertenece a otro usuario se comporta igual que un id inexistente (404).
     */
    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndIdProcesoContrato(Long usuarioId, String idProcesoContrato);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    @Modifying
    @Transactional
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :usuarioId AND n.leida = false")
    void marcarTodasLeidas(@Param("usuarioId") Long usuarioId);
}
