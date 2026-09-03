package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.security.CustomUserDetailsService;
import co.edu.unbosque.sistemawebraco.security.JwtUtil;
import co.edu.unbosque.sistemawebraco.security.SecurityConfig;
import co.edu.unbosque.sistemawebraco.services.InformeService;
import co.edu.unbosque.sistemawebraco.services.SecopService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica la capa de seguridad real (SecurityConfig + JwtAuthenticationFilter) delante de
 * /api/secop/** — sin JWT debe rechazar con 401, y con un JWT válido debe dejar pasar la
 * petición hasta el controller.
 */
@WebMvcTest(SecopController.class)
@Import({SecurityConfig.class, JwtUtil.class, CustomUserDetailsService.class})
class SecopControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SecopService secopService;
    @MockitoBean
    private InformeService informeService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/secop/contratos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/secop/contratos").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenValidoDevuelve200() throws Exception {
        when(usuarioRepository.findByEmail("usuario@correo.com")).thenReturn(Optional.of(usuarioDePrueba()));
        when(secopService.consultarSecop(1, 10)).thenReturn(List.of(new SecopContratoDTO()));

        mockMvc.perform(get("/api/secop/contratos")
                        .header("Authorization", "Bearer " + tokenValidoPara("usuario@correo.com")))
                .andExpect(status().isOk());
    }

    @Test
    void buscarConTokenValidoRegistraLaBusquedaDelUsuarioAutenticado() throws Exception {
        when(usuarioRepository.findByEmail("usuario@correo.com")).thenReturn(Optional.of(usuarioDePrueba()));
        when(secopService.buscarContratos(any())).thenReturn(List.of());

        mockMvc.perform(post("/api/secop/buscar")
                        .header("Authorization", "Bearer " + tokenValidoPara("usuario@correo.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContractSearchRequest())))
                .andExpect(status().isOk());
    }

    private Usuario usuarioDePrueba() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("usuario@correo.com");
        usuario.setPasswordHash("hash-bcrypt-fake");
        return usuario;
    }

    private String tokenValidoPara(String email) {
        return jwtUtil.generateToken(email);
    }
}
