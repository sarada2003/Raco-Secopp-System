package co.edu.unbosque.sistemawebraco.mapper;

import co.edu.unbosque.sistemawebraco.dto.UsuarioDTO;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Componente responsable de la conversión bidireccional entre la entidad JPA
 * {@link Usuario} y el objeto de transferencia de datos {@link UsuarioDTO}.
 *
 * <p>Separa la capa de persistencia de la capa de presentación/API, asegurando que:</p>
 * <ul>
 *   <li>La <b>contraseña hasheada</b> ({@code passwordHash}) nunca se exponga en
 *       las respuestas HTTP — {@link #toDTO(Usuario)} la omite deliberadamente.</li>
 *   <li>Los campos de auditoría ({@code activo}, {@code fechaRegistro}) se inicialicen
 *       con valores por defecto seguros al crear un nuevo usuario.</li>
 * </ul>
 *
 * <p>Los métodos son {@code static} para facilitar su uso sin necesidad de inyección,
 * aunque la clase está registrada como {@code @Component} para que Spring pueda
 * inyectarla donde se requiera como instancia.</p>
 *
 * @author Proyecto de Grado — Universidad El Bosque
 * @see Usuario
 * @see UsuarioDTO
 */
@Component
public class UsuarioMapper {

    /**
     * Convierte un {@link UsuarioDTO} (datos del cliente) a la entidad JPA {@link Usuario}
     * lista para ser persistida en la base de datos.
     *
     * <p><b>Campos mapeados:</b> {@code nombre}, {@code email}.</p>
     * <p><b>Campos inicializados con defaults:</b>
     * <ul>
     *   <li>{@code activo} → {@code true} (toda cuenta nueva se crea activa)</li>
     *   <li>{@code fechaRegistro} → momento actual ({@link LocalDateTime#now()})</li>
     * </ul>
     * <p><b>Campos NO mapeados:</b> {@code password} del DTO — la contraseña debe
     * encriptarse con BCrypt <em>después</em> de llamar a este método, antes de persistir.</p>
     *
     * @param dto datos del usuario provenientes del cliente; puede ser {@code null}
     * @return nueva instancia de {@link Usuario} lista para guardar, o {@code null} si el DTO es nulo
     */
    public static Usuario toEntity(UsuarioDTO dto) {
        if (dto == null) return null;

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setActivo(true);
        usuario.setFechaRegistro(LocalDateTime.now());
        return usuario;
    }

    /**
     * Convierte una entidad JPA {@link Usuario} (datos de la base de datos) a
     * {@link UsuarioDTO} para ser enviado en las respuestas HTTP.
     *
     * <p><b>Campos mapeados:</b> {@code id}, {@code nombre}, {@code email},
     * {@code activo}, {@code fechaRegistro}.</p>
     * <p><b>Campo explícitamente omitido:</b> {@code passwordHash} — nunca se incluye
     * en el DTO para evitar su exposición en la API.</p>
     *
     * @param usuario entidad recuperada de la base de datos; puede ser {@code null}
     * @return {@link UsuarioDTO} con los datos públicos del usuario, o {@code null} si la entidad es nula
     */
    public static UsuarioDTO toDTO(Usuario usuario) {
        if (usuario == null) return null;

        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .activo(usuario.isActivo())
                .fechaRegistro(usuario.getFechaRegistro())
                .build();
    }
}
