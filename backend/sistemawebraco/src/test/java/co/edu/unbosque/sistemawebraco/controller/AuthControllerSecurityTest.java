package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.dto.UsuarioDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginRequestDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginResponseDTO;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.security.CustomUserDetailsService;
import co.edu.unbosque.sistemawebraco.security.JwtUtil;
import co.edu.unbosque.sistemawebraco.security.SecurityConfig;
import co.edu.unbosque.sistemawebraco.services.AuthService;
import co.edu.unbosque.sistemawebraco.services.SecopService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/auth/login y /api/auth/register son las únicas rutas públicas del sistema
 * (SecurityConfig.permitAll); /api/auth/me exige JWT igual que el resto de la API.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtUtil.class, CustomUserDetailsService.class})
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private SecopService secopService; // inyectado en el controller aunque no se use en /login ni /register
    @MockitoBean
    private UsuarioRepository usuarioRepository; // requerido por CustomUserDetailsService al validar el JWT de /me

    @Test
    void loginSinTokenEsPublicoYRespondeSegunElServicio() throws Exception {
        LoginRequestDTO request = LoginRequestDTO.builder().email("usuario@correo.com").password("clave123").build();
        when(authService.login(any())).thenReturn(new LoginResponseDTO());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void registerSinTokenEsPublico() throws Exception {
        when(authService.register(any())).thenReturn(new UsuarioDTO());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan\",\"email\":\"juan@correo.com\",\"password\":\"clave123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void meSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meConTokenValidoDevuelve200() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("usuario@correo.com");
        usuario.setPasswordHash("hash");
        when(usuarioRepository.findByEmail("usuario@correo.com")).thenReturn(Optional.of(usuario));
        when(authService.obtenerUsuarioDesdeToken(any())).thenReturn(new UsuarioDTO());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuario@correo.com")))
                .andExpect(status().isOk());
    }
}
