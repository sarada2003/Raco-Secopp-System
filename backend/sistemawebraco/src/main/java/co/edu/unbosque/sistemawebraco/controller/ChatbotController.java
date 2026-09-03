package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.services.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "Asistente IA para contratación pública (gpt-4o-mini)")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Recibe un mensaje del usuario y retorna la respuesta del chatbot.
     *
     * <p>Requiere JWT válido en el header {@code Authorization: Bearer <token>}.</p>
     *
     * @param body JSON con campo {@code mensaje} (1–2000 caracteres)
     * @return JSON con campo {@code respuesta}
     */
    @Operation(summary = "Enviar mensaje al chatbot SECOP",
               description = "Procesa el mensaje del usuario con gpt-4o-mini y retorna una respuesta sobre contratación pública.")
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@Valid @RequestBody ChatRequest body, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        String respuesta = chatbotService.chat(usuario, body.mensaje());
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }

    record ChatRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
        String mensaje
    ) {}
}
