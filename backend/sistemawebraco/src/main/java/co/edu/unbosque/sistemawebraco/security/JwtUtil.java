package co.edu.unbosque.sistemawebraco.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidad para la creación, validación y extracción de información de tokens JWT.
 *
 * <p>Implementa el estándar <b>JSON Web Token (JWT)</b> usando el algoritmo de firma
 * simétrica <b>HS512</b> (HMAC-SHA512). La clave secreta se inyecta desde la variable
 * de entorno {@code JWT_SECRET} definida en {@code application.properties}.</p>
 *
 * <h3>Estructura del token generado</h3>
 * <ul>
 *   <li><b>Header:</b> {@code {"alg":"HS512"}}</li>
 *   <li><b>Payload:</b> {@code {"sub":"email@usuario.com", "iat":..., "exp":...}}</li>
 *   <li><b>Signature:</b> HMAC-SHA512 de header + payload usando la clave secreta</li>
 * </ul>
 *
 * <h3>Expiración</h3>
 * <p>Configurable mediante {@code jwt.expiration} en {@code application.properties}
 * (en milisegundos). Por defecto: {@code 86400000} ms = 24 horas.</p>
 *
 * @author Proyecto de Grado — Universidad El Bosque
 * @see JwtAuthenticationFilter
 */
@Component
public class JwtUtil {

    /** Clave secreta derivada del valor de {@code jwt.secret}, usada para firmar y verificar tokens. */
    private final SecretKey secretKey;

    /** Tiempo de validez del token en milisegundos, leído de {@code jwt.expiration}. */
    private final long expirationMillis;

    /**
     * Construye el utilitario JWT inyectando la clave secreta y el tiempo de expiración
     * desde {@code application.properties}.
     *
     * <p>La clave secreta se convierte directamente desde UTF-8 sin decodificación Base64,
     * garantizando compatibilidad con el valor definido en la variable de entorno.</p>
     *
     * @param secret          valor de {@code jwt.secret} (mínimo 64 caracteres para HS512)
     * @param expirationMillis valor de {@code jwt.expiration} en milisegundos
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expirationMillis) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /**
     * Genera un token JWT firmado para el usuario indicado.
     *
     * <p>El claim {@code sub} (subject) almacena el email del usuario.
     * El token incluye la fecha de emisión ({@code iat}) y la fecha de expiración ({@code exp}).</p>
     *
     * @param username email del usuario que se incluirá como subject del token
     * @return token JWT compacto en formato {@code header.payload.signature}
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Valida que un token JWT sea auténtico y no haya expirado.
     *
     * <p>Verifica la firma HMAC-SHA512 con la clave secreta configurada.
     * Captura cualquier {@link JwtException} (token malformado, expirado, firma inválida)
     * y retorna {@code false} en lugar de propagar la excepción.</p>
     *
     * @param token token JWT a validar (sin el prefijo "Bearer ")
     * @return {@code true} si el token es válido y vigente; {@code false} en caso contrario
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Extrae el email del usuario (claim {@code sub}) de un token JWT válido.
     *
     * <p><b>Precondición:</b> debe llamarse solo después de confirmar que el token es
     * válido con {@link #validateToken(String)}, ya que este método lanza excepción
     * si el token está expirado o malformado.</p>
     *
     * @param token token JWT válido (sin el prefijo "Bearer ")
     * @return email del usuario almacenado en el claim {@code sub}
     * @throws JwtException si el token es inválido o está expirado
     */
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
