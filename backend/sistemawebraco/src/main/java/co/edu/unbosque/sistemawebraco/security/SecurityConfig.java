package co.edu.unbosque.sistemawebraco.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuración central de Spring Security para el sistema RACO–SECOPP.
 *
 * <p>Define la cadena de filtros de seguridad, la política de sesiones, las reglas de
 * autorización por endpoint, el filtro JWT personalizado, el cifrado de contraseñas
 * y la configuración de CORS.</p>
 *
 * <h3>Política de seguridad</h3>
 * <ul>
 *   <li><b>Stateless:</b> no se usan sesiones HTTP; cada petición se autentica
 *       individualmente mediante el JWT presente en el header {@code Authorization}.</li>
 *   <li><b>CSRF deshabilitado:</b> innecesario en APIs REST sin estado con autenticación
 *       por token.</li>
 * </ul>
 *
 * <h3>Endpoints públicos (sin JWT)</h3>
 * <ul>
 *   <li>{@code POST /api/auth/login}</li>
 *   <li>{@code POST /api/auth/register}</li>
 *   <li>{@code GET /swagger-ui/**} y {@code GET /v3/api-docs/**} — documentación Swagger</li>
 * </ul>
 *
 * <h3>Endpoints protegidos (requieren JWT válido)</h3>
 * <ul>
 *   <li>{@code /api/secop/**} — consultas a SECOP II</li>
 *   <li>Cualquier otro endpoint no listado explícitamente</li>
 * </ul>
 *
 * @author Proyecto de Grado — Universidad El Bosque
 * @see JwtAuthenticationFilter
 * @see JwtUtil
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * @param jwtUtil            utilidad JWT para crear el filtro de autenticación
     * @param userDetailsService servicio de carga de usuarios por email
     */
    public SecurityConfig(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Define la cadena principal de filtros de seguridad HTTP.
     *
     * <p>Registra el {@link JwtAuthenticationFilter} antes del filtro estándar de
     * autenticación por usuario/contraseña de Spring, de modo que las peticiones con
     * JWT válido se autentican sin pasar por el formulario de login.</p>
     *
     * @param http objeto de configuración de Spring Security
     * @return cadena de filtros construida
     * @throws Exception si ocurre un error durante la configuración
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);

        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/secop/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Bean de codificación de contraseñas con el algoritmo <b>BCrypt</b>.
     *
     * <p>BCrypt es un algoritmo de hashing adaptativo que incluye un salt aleatorio
     * en cada hash, haciendo que dos hashes de la misma contraseña sean siempre distintos.
     * Spring Security lo usa automáticamente al llamar a
     * {@link PasswordEncoder#matches(CharSequence, String)}.</p>
     *
     * @return instancia de {@link BCryptPasswordEncoder} con factor de costo por defecto (10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean de filtro CORS (Cross-Origin Resource Sharing) que permite peticiones
     * desde el frontend Angular en desarrollo.
     *
     * <p>Configuración actual:</p>
     * <ul>
     *   <li><b>Origen permitido:</b> {@code http://localhost:4200} (Angular dev server)</li>
     *   <li><b>Métodos permitidos:</b> GET, POST, PUT, DELETE, OPTIONS</li>
     *   <li><b>Headers permitidos:</b> Origin, Content-Type, Accept, Authorization</li>
     *   <li><b>Credentials:</b> habilitadas (necesario para enviar el header Authorization)</li>
     * </ul>
     *
     * @return filtro CORS configurado para el entorno de desarrollo
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedHeaders(List.of("Origin", "Content-Type", "Accept", "Authorization"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
