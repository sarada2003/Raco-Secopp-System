package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.security.CustomUserDetailsService;
import co.edu.unbosque.sistemawebraco.security.JwtUtil;
import co.edu.unbosque.sistemawebraco.security.SecurityConfig;
import co.edu.unbosque.sistemawebraco.services.NotificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/notificaciones/** requiere JWT, y el id de usuario usado para todas las
 * operaciones (incluida la verificación de dueño) sale siempre del token — nunca de un
 * parámetro que el cliente pueda manipular. Estos tests prueban ambas cosas contra la
 * cadena de seguridad real.
 */
@WebMvcTest(NotificacionController.class)
@Import({SecurityConfig.class, JwtUtil.class, CustomUserDetailsService.class})
class NotificacionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private NotificacionService notificacionService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());
    }

    @Test
    void marcarLeidaUsaSiempreElIdDelUsuarioAutenticadoNoUnoQueEnvieElCliente() throws Exception {
        Usuario usuarioA = new Usuario();
        usuarioA.setId(1L);
        usuarioA.setEmail("usuarioA@correo.com");
        usuarioA.setPasswordHash("hash");
        when(usuarioRepository.findByEmail("usuarioA@correo.com")).thenReturn(Optional.of(usuarioA));

        // Usuario A pide marcar como leída la notificación 99 (que podría ser de cualquier
        // dueño — el path solo tiene el id de la notificación, nunca un id de usuario).
        mockMvc.perform(patch("/api/notificaciones/99/leida")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuarioA@correo.com")))
                .andExpect(status().isOk());

        // El servicio (que sí hace el chequeo de dueño en la query) se llama con el id
        // del usuario autenticado por el JWT — jamás con uno provisto por el cliente.
        verify(notificacionService).marcarLeida(99L, 1L);
    }

    @Test
    void listarSoloConsultaLasNotificacionesDelUsuarioDelToken() throws Exception {
        Usuario usuarioB = new Usuario();
        usuarioB.setId(2L);
        usuarioB.setEmail("usuarioB@correo.com");
        usuarioB.setPasswordHash("hash");
        when(usuarioRepository.findByEmail("usuarioB@correo.com")).thenReturn(Optional.of(usuarioB));

        mockMvc.perform(get("/api/notificaciones")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuarioB@correo.com")))
                .andExpect(status().isOk());

        verify(notificacionService).listar(2L);
    }
}
