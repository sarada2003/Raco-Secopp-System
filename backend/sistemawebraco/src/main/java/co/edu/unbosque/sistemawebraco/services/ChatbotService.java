package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.entity.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class ChatbotService {

    private final ChatClient chatClient;

    /*
     * Virtual threads (Java 21) en lugar del ForkJoinPool común: permiten blocking
     * sin riesgo de thread starvation y son compatibles con el bloqueo reactivo
     * interno que hace Spring AI durante el function calling.
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ChatbotService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        Eres RACO, un asistente especializado en contratación pública colombiana y el sistema SECOP II.

                        Tienes acceso a la herramienta 'buscarContratosSecop' que consulta contratos REALES
                        publicados en SECOP II (datos.gov.co) en tiempo real.

                        REGLAS:
                        - Cuando el usuario quiera buscar, ver, consultar, encontrar o listar contratos del Estado,
                          SIEMPRE usa la herramienta 'buscarContratosSecop'.
                        - Extrae del mensaje los filtros que puedas: palabraClave, entidad, valorMin.
                          Si no se menciona algún filtro, no lo incluyas (déjalo null).
                        - Presenta los resultados de forma clara, amigable y en español.
                        - Para preguntas generales sobre contratación pública (qué es SECOP, tipos de contratos,
                          cómo participar, etc.) responde directamente sin usar la herramienta.
                        - Responde siempre en español.
                        """)
                .build();
    }

    public String chat(Usuario usuario, String mensaje) {
        Future<String> future = executor.submit(() -> {
            ChatContext.set(usuario);
            try {
                ChatResponse response = chatClient.prompt()
                    .user(mensaje)
                    .options(OpenAiChatOptions.builder()
                        .toolNames("buscarContratosSecop")
                        .build())
                    .call()
                    .chatResponse();
                logTokenUsage(response);
                return response.getResult().getOutput().getText();
            } finally {
                ChatContext.clear();
            }
        });

        try {
            return future.get(90, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return "El asistente tardó demasiado en responder. Intenta con una pregunta más específica o en un momento.";
        } catch (Exception e) {
            return "Ocurrió un error al procesar tu consulta. Por favor intenta de nuevo.";
        }
    }

    /**
     * Registra el consumo de tokens de la interacción para poder reportar rendimiento/costo.
     *
     * <p>Solo se loguean conteos numéricos (prompt/completion/total). El mensaje del usuario
     * y la respuesta del modelo nunca se incluyen aquí: los usuarios consultan contratos
     * reales a través del chatbot y este log no debe convertirse en un registro de esas
     * conversaciones.</p>
     */
    private void logTokenUsage(ChatResponse response) {
        Usage usage = response.getMetadata().getUsage();
        log.info("Chatbot tokens - prompt={}, completion={}, total={}",
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }
}
