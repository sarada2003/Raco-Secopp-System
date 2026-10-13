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

import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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

    // --- POST /api/chatbot/chat/stream (SSE) ---

    @Test
    void streamSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/chatbot/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"hola\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamConMensajeVacioDevuelve400() throws Exception {
        stubUsuario("usuario@correo.com");

        mockMvc.perform(post("/api/chatbot/chat/stream")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuario@correo.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    /** Cubre también el dispatch ASYNC: sin el permitAll de SecurityConfig, aquí respondería 401/403. */
    @Test
    void streamValidoEnviaLosFragmentosYUnEventoFin() throws Exception {
        stubUsuario("usuario@correo.com");
        when(chatbotService.chatStream(any(), any())).thenReturn(Flux.just("Hola", ", ¿en qué te ayudo?"));

        var resultado = mockMvc.perform(post("/api/chatbot/chat/stream")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuario@correo.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"hola\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        resultado.getResponse().setCharacterEncoding("UTF-8"); // SSE siempre va en UTF-8

        mockMvc.perform(asyncDispatch(resultado))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("data:{\"texto\":\"Hola\"}")))
                .andExpect(content().string(containsString("data:{\"texto\":\", ¿en qué te ayudo?\"}")))
                .andExpect(content().string(containsString("event:fin")))
                .andExpect(content().string(not(containsString("event:error"))));
    }

    @Test
    void streamConTimeoutEnviaEventoDeErrorConMensajeAmigable() throws Exception {
        stubUsuario("usuario@correo.com");
        when(chatbotService.chatStream(any(), any())).thenReturn(Flux.error(new TimeoutException()));

        var resultado = mockMvc.perform(post("/api/chatbot/chat/stream")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("usuario@correo.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"hola\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();
        resultado.getResponse().setCharacterEncoding("UTF-8"); // SSE siempre va en UTF-8

        mockMvc.perform(asyncDispatch(resultado))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:error")))
                .andExpect(content().string(containsString(ChatbotService.MENSAJE_TIMEOUT)))
                .andExpect(content().string(not(containsString("event:fin"))));
    }
}
