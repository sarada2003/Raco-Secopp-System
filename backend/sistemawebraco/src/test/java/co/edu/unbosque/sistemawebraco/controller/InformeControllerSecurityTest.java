package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.dto.InformeDetalleDTO;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.security.CustomUserDetailsService;
import co.edu.unbosque.sistemawebraco.security.JwtUtil;
import co.edu.unbosque.sistemawebraco.security.SecurityConfig;
import co.edu.unbosque.sistemawebraco.services.InformeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InformeController.class)
@Import({SecurityConfig.class, JwtUtil.class, CustomUserDetailsService.class})
class InformeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private InformeService informeService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private void stubUsuario(String email, long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setPasswordHash("hash");
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/informes")).andExpect(status().isUnauthorized());
    }

    @Test
    void obtenerUnInformeDeOtroUsuarioResponde404() throws Exception {
        stubUsuario("usuario@correo.com", 1L);
        when(informeService.obtener(5L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Informe no encontrado"));

        mockMvc.perform(get("/api/informes/5")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuario@correo.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerUnInformePropioDevuelve200YUsaElIdDelToken() throws Exception {
        stubUsuario("usuario@correo.com", 1L);
        when(informeService.obtener(5L, 1L)).thenReturn(new InformeDetalleDTO());

        mockMvc.perform(get("/api/informes/5")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuario@correo.com")))
                .andExpect(status().isOk());

        verify(informeService).obtener(5L, 1L);
    }
}
