package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.services.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "Asistente IA para contratación pública (gpt-4o-mini)")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatbotService chatbotService;

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
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest body) {
        String respuesta = chatbotService.chat(body.mensaje());
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }

    record ChatRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 2000, message = "El mensaje no puede superar los 2000 caracteres")
        String mensaje
    ) {}
}
