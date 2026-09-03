package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.UsuarioDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginRequestDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginResponseDTO;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.mapper.UsuarioMapper;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(authService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(authService, "usuarioMapper", new UsuarioMapper());
    }

    private Usuario usuarioConHash(String hash) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("usuario@correo.com");
        usuario.setNombre("Juan Pérez");
        usuario.setPasswordHash(hash);
        return usuario;
    }

    private LoginRequestDTO loginRequest(String email, String password) {
        return LoginRequestDTO.builder().email(email).password(password).build();
    }

    @Test
    void loginExitosoDevuelveTokenYUsuarioSinPassword() {
        Usuario usuario = usuarioConHash("hash-bcrypt");
        when(usuarioRepository.findByEmail("usuario@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave123", "hash-bcrypt")).thenReturn(true);
        when(jwtUtil.generateToken("usuario@correo.com")).thenReturn("token-jwt");

        LoginRequestDTO request = loginRequest("usuario@correo.com", "clave123");

        LoginResponseDTO response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("token-jwt");
        assertThat(response.getUsuario().getEmail()).isEqualTo("usuario@correo.com");
        assertThat(response.getUsuario().getPassword()).isNull();
    }

    @Test
    void loginConEmailInexistenteLanzaExcepcion() {
        when(usuarioRepository.findByEmail("no-existe@correo.com")).thenReturn(Optional.empty());

        LoginRequestDTO request = loginRequest("no-existe@correo.com", "clave123");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(RuntimeException.class);
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void loginConPasswordIncorrectoLanzaExcepcion() {
        Usuario usuario = usuarioConHash("hash-bcrypt");
        when(usuarioRepository.findByEmail("usuario@correo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-mala", "hash-bcrypt")).thenReturn(false);

        LoginRequestDTO request = loginRequest("usuario@correo.com", "clave-mala");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(RuntimeException.class);
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void registerConEmailNuevoEncriptaLaPasswordYGuarda() {
        when(usuarioRepository.findByEmail("nuevo@correo.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("clave123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre("Nuevo Usuario");
        dto.setEmail("nuevo@correo.com");
        dto.setPassword("clave123");

        UsuarioDTO registrado = authService.register(dto);

        assertThat(registrado.getId()).isEqualTo(2L);
        assertThat(registrado.getEmail()).isEqualTo("nuevo@correo.com");
        verify(passwordEncoder).encode("clave123");
    }

    @Test
    void registerConEmailYaExistenteLanzaExcepcion() {
        when(usuarioRepository.findByEmail("existe@correo.com")).thenReturn(Optional.of(usuarioConHash("x")));

        UsuarioDTO dto = new UsuarioDTO();
        dto.setEmail("existe@correo.com");
        dto.setPassword("clave123");

        assertThatThrownBy(() -> authService.register(dto)).isInstanceOf(RuntimeException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void obtenerUsuarioDesdeTokenSinPrefijoBearerLanzaExcepcion() {
        assertThatThrownBy(() -> authService.obtenerUsuarioDesdeToken("token-sin-prefijo"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> authService.obtenerUsuarioDesdeToken(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void obtenerUsuarioDesdeTokenValidoDevuelveElUsuario() {
        when(jwtUtil.extractUsername("jwt-valido")).thenReturn("usuario@correo.com");
        when(usuarioRepository.findByEmail("usuario@correo.com")).thenReturn(Optional.of(usuarioConHash("x")));

        UsuarioDTO dto = authService.obtenerUsuarioDesdeToken("Bearer jwt-valido");

        assertThat(dto.getEmail()).isEqualTo("usuario@correo.com");
    }

    @Test
    void loginConGoogleConEmailNoRegistradoLanzaExcepcion() {
        when(usuarioRepository.findByEmail("nadie@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginConGoogle("nadie@correo.com")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void loginConGoogleConEmailRegistradoGeneraToken() {
        Usuario usuario = usuarioConHash("hash-bcrypt");
        when(usuarioRepository.findByEmail("usuario@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generateToken("usuario@correo.com")).thenReturn("token-google");

        LoginResponseDTO response = authService.loginConGoogle("usuario@correo.com");

        assertThat(response.getToken()).isEqualTo("token-google");
    }
}
