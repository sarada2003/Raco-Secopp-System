package co.edu.unbosque.sistemawebraco.repository;

import co.edu.unbosque.sistemawebraco.entity.Informe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InformeRepository extends JpaRepository<Informe, Long> {

    List<Informe> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    /** Verificación de dueño en la propia consulta — mismo patrón que Notificacion/SuscripcionAlerta. */
    Optional<Informe> findByIdAndUsuarioId(Long id, Long usuarioId);
}
