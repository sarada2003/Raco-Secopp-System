package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.security.CustomUserDetailsService;
import co.edu.unbosque.sistemawebraco.security.JwtUtil;
import co.edu.unbosque.sistemawebraco.security.SecurityConfig;
import co.edu.unbosque.sistemawebraco.services.ChatbotService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatbotController.class)
@Import({SecurityConfig.class, JwtUtil.class, CustomUserDetailsService.class})
class ChatbotControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private ChatbotService chatbotService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private void stubUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail(email);
        usuario.setPasswordHash("hash");
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/chatbot/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"hola\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mensajeVacioDevuelve400ConTokenValido() throws Exception {
        stubUsuario("usuario@correo.com");

        mockMvc.perform(post("/api/chatbot/chat")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuario@correo.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mensajeMayorA2000CaracteresDevuelve400() throws Exception {
        stubUsuario("usuario@correo.com");
        String mensajeLargo = "a".repeat(2001);

        mockMvc.perform(post("/api/chatbot/chat")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuario@correo.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"" + mensajeLargo + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mensajeValidoConTokenValidoDevuelve200() throws Exception {
        stubUsuario("usuario@correo.com");
        when(chatbotService.chat(any(), any())).thenReturn("Respuesta del asistente");

        mockMvc.perform(post("/api/chatbot/chat")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuario@correo.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"Busca contratos de obras\"}"))
                .andExpect(status().isOk());
    }
}
