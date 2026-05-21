package co.edu.unbosque.sistemawebraco.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad HTTP que intercepta cada petición y autentica al usuario
 * a partir del token JWT presente en el header {@code Authorization}.
 *
 * <p>Extiende {@link OncePerRequestFilter} para garantizar que la lógica de
 * autenticación se ejecuta <b>exactamente una vez por petición</b>, evitando
 * duplicaciones en cadenas de filtros complejas.</p>
 *
 * <h3>Flujo de autenticación por petición</h3>
 * <ol>
 *   <li>Extrae el header {@code Authorization} de la petición HTTP.</li>
 *   <li>Si el valor comienza con {@code "Bearer "}, extrae el JWT.</li>
 *   <li>Valida el token con {@link JwtUtil#validateToken(String)} (firma + expiración).</li>
 *   <li>Si el token es válido, extrae el email del subject y carga el {@link UserDetails}.</li>
 *   <li>Si el contexto de seguridad aún no tiene autenticación, registra el usuario
 *       en el {@link SecurityContextHolder}.</li>
 *   <li>Siempre pasa la petición al siguiente filtro de la cadena.</li>
 * </ol>
 *
 * <p>Si el header está ausente o el token es inválido/expirado, la petición continúa
 * sin autenticación — Spring Security rechazará el acceso a los endpoints protegidos
 * devolviendo {@code 401 Unauthorized}.</p>
 *
 * @author Proyecto de Grado — Universidad El Bosque
 * @see JwtUtil
 * @see SecurityConfig
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * @param jwtUtil            utilidad para validar y parsear tokens JWT
     * @param userDetailsService servicio para cargar los datos del usuario por email
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Lógica principal del filtro: evalúa el JWT y, si es válido, establece
     * la autenticación en el {@link SecurityContextHolder}.
     *
     * @param request     petición HTTP entrante
     * @param response    respuesta HTTP
     * @param filterChain cadena de filtros de Spring Security
     * @throws ServletException si ocurre un error en el procesamiento del servlet
     * @throws IOException      si ocurre un error de I/O durante el filtrado
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);

            // Validar primero para evitar que extractUsername lance excepción con tokens expirados
            if (jwtUtil.validateToken(jwt)) {
                String username = jwtUtil.extractUsername(jwt);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
