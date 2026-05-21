package co.edu.unbosque.sistemawebraco.security;

import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link UserDetailsService} que carga los datos del usuario
 * desde PostgreSQL usando el email como identificador único.
 *
 * <p>Spring Security utiliza este servicio internamente durante la validación del JWT:
 * una vez que el {@link JwtAuthenticationFilter} extrae el email del token, llama a
 * {@link #loadUserByUsername(String)} para obtener el {@link UserDetails} y construir
 * el contexto de autenticación.</p>
 *
 * <p>El campo {@code username} de Spring Security se mapea al {@code email} del
 * sistema RACO, ya que el email es el identificador único de login.</p>
 *
 * @author Proyecto de Grado — Universidad El Bosque
 * @see JwtAuthenticationFilter
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * @param usuarioRepository repositorio JPA para consultar usuarios en PostgreSQL
     */
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carga los detalles de seguridad del usuario a partir de su email.
     *
     * <p>Busca al usuario en la base de datos por email. Si lo encuentra, construye
     * un objeto {@link UserDetails} de Spring Security con:</p>
     * <ul>
     *   <li>{@code username} → email del usuario</li>
     *   <li>{@code password} → hash BCrypt almacenado en la base de datos</li>
     *   <li>{@code roles} → {@code ["USER"]} (rol único por defecto en este sistema)</li>
     * </ul>
     *
     * @param email email del usuario (usado como username en Spring Security)
     * @return {@link UserDetails} con las credenciales y roles del usuario
     * @throws UsernameNotFoundException si no existe un usuario con ese email en la base de datos
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .roles("USER")
                .build();
    }
}
