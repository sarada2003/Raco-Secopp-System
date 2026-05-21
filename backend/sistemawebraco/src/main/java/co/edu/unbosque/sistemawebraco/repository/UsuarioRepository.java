package co.edu.unbosque.sistemawebraco.repository;

import co.edu.unbosque.sistemawebraco.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Usuario}.
 *
 * <p>Extiende {@link JpaRepository} para heredar automáticamente las operaciones CRUD
 * estándar ({@code save}, {@code findById}, {@code findAll}, {@code delete}, etc.)
 * sin necesidad de implementación manual.</p>
 *
 * <p>Spring Data JPA genera la implementación en tiempo de ejecución a partir de
 * la interfaz. La base de datos subyacente es <b>PostgreSQL</b>, configurada en
 * {@code application.properties}.</p>
 *
 * <p><b>Clave primaria:</b> {@code Integer} que mapea al campo {@code id} de tipo
 * {@code BIGINT} en la tabla {@code Usuario} de PostgreSQL.</p>
 *
 * @author Proyecto de Grado — Universidad El Bosque
 * @see Usuario
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Busca un usuario por su dirección de email.
     *
     * <p>Spring Data JPA deriva la consulta SQL automáticamente del nombre del método:
     * {@code SELECT * FROM usuario WHERE email = ?1}.</p>
     *
     * <p>Se usa en los flujos de:</p>
     * <ul>
     *   <li>Login manual — verificar existencia y obtener el hash BCrypt</li>
     *   <li>Registro — validar unicidad del email antes de insertar</li>
     *   <li>Login con Google — encontrar la cuenta asociada al email de Google</li>
     *   <li>Carga de {@code UserDetails} en el filtro JWT</li>
     * </ul>
     *
     * @param email dirección de email a buscar (sensible a mayúsculas según la BD)
     * @return {@link Optional} con el usuario si existe, o vacío si no se encuentra
     */
    Optional<Usuario> findByEmail(String email);
}
