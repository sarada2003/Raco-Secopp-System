package co.edu.unbosque.sistemawebraco.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    // HS512 requiere una clave de al menos 64 bytes.
    private static final String SECRET = "test-secret-key-para-jwt-util-test-necesita-al-menos-64-caracteres-1234567890";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 86_400_000L);

    @Test
    void generaUnTokenNoVacio() {
        String token = jwtUtil.generateToken("usuario@correo.com");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    void validaUnTokenRecienGenerado() {
        String token = jwtUtil.generateToken("usuario@correo.com");

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void extraeElEmailDelSubject() {
        String token = jwtUtil.generateToken("usuario@correo.com");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("usuario@correo.com");
    }

    @Test
    void tokenExpiradoNoEsValido() {
        // Expiración negativa: la fecha de expiración queda en el pasado inmediatamente.
        JwtUtil jwtExpirado = new JwtUtil(SECRET, -1_000L);
        String token = jwtExpirado.generateToken("usuario@correo.com");

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void extractUsernameLanzaExcepcionConTokenExpirado() {
        JwtUtil jwtExpirado = new JwtUtil(SECRET, -1_000L);
        String token = jwtExpirado.generateToken("usuario@correo.com");

        assertThatThrownBy(() -> jwtUtil.extractUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFirmadoConOtraClaveNoEsValido() {
        JwtUtil otroEmisor = new JwtUtil("otra-clave-completamente-distinta-tambien-de-al-menos-64-caracteres-000000", 86_400_000L);
        String token = otroEmisor.generateToken("usuario@correo.com");

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void tokenMalformadoNoEsValido() {
        assertThat(jwtUtil.validateToken("esto-no-es-un-jwt")).isFalse();
    }

    @Test
    void tokenVacioNoEsValido() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }
}
