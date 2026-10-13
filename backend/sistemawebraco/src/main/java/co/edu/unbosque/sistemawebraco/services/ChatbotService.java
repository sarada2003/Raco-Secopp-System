package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.entity.Usuario;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ChatbotService {

    /** Clave del {@code ToolContext} con la que la herramienta 'buscarContratosSecop' recibe al usuario autenticado. */
    public static final String TOOL_CONTEXT_USUARIO = "usuario";

    public static final String MENSAJE_TIMEOUT =
            "El asistente tardó demasiado en responder. Intenta con una pregunta más específica o en un momento.";
    public static final String MENSAJE_ERROR =
            "Ocurrió un error al procesar tu consulta. Por favor intenta de nuevo.";

    private static final Duration TIMEOUT_RESPUESTA = Duration.ofSeconds(90);
    /** En streaming: tiempo máximo sin recibir un nuevo fragmento una vez el modelo ya empezó a escribir. */
    private static final Duration TIMEOUT_ENTRE_FRAGMENTOS = Duration.ofSeconds(30);

    private final ChatClient chatClient;
    private final MeterRegistry meterRegistry;

    /*
     * Virtual threads (Java 21) en lugar del ForkJoinPool común: permiten blocking
     * sin riesgo de thread starvation y son compatibles con el bloqueo reactivo
     * interno que hace Spring AI durante el function calling.
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ChatbotService(ChatClient.Builder builder, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
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

    /** Respuesta completa en un solo bloque (POST /api/chatbot/chat). */
    public String chat(Usuario usuario, String mensaje) {
        Timer.Sample muestra = Timer.start(meterRegistry);
        String resultado = "ok";

        Future<String> future = executor.submit(() -> {
            ChatResponse response = prompt(usuario, mensaje, false).call().chatResponse();
            logTokenUsage(response.getMetadata().getUsage());
            return response.getResult().getOutput().getText();
        });

        try {
            return future.get(TIMEOUT_RESPUESTA.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            resultado = "timeout";
            future.cancel(true);
            return MENSAJE_TIMEOUT;
        } catch (Exception e) {
            resultado = "error";
            log.error("Chatbot falló: {}", e.getMessage());
            return MENSAJE_ERROR;
        } finally {
            muestra.stop(timerRespuesta("completa", resultado));
        }
    }

    /**
     * Respuesta en fragmentos a medida que el modelo la genera (POST /api/chatbot/chat/stream).
     *
     * <p>El modelo, el prompt y la herramienta son los mismos que en {@link #chat}: el contenido de
     * la respuesta no cambia, solo deja de esperarse a que esté completa para empezar a mostrarla.
     * Los errores (incluido el timeout) se propagan como señal de error del {@link Flux}; el
     * controlador los traduce a un evento para el frontend.</p>
     */
    public Flux<String> chatStream(Usuario usuario, String mensaje) {
        return Flux.defer(() -> {
            Timer.Sample muestra = Timer.start(meterRegistry);
            long inicio = System.nanoTime();
            AtomicBoolean primerFragmento = new AtomicBoolean(true);
            AtomicReference<Usage> usage = new AtomicReference<>();

            return prompt(usuario, mensaje, true).stream().chatResponse()
                    .doOnNext(response -> guardarUsage(response, usage))
                    .mapNotNull(ChatbotService::textoDe)
                    .filter(texto -> !texto.isEmpty())
                    .doOnNext(texto -> {
                        if (primerFragmento.compareAndSet(true, false)) {
                            meterRegistry.timer("chatbot.primer.fragmento")
                                    .record(Duration.ofNanos(System.nanoTime() - inicio));
                        }
                    })
                    .timeout(Mono.delay(TIMEOUT_RESPUESTA), texto -> Mono.delay(TIMEOUT_ENTRE_FRAGMENTOS))
                    .doOnComplete(() -> {
                        muestra.stop(timerRespuesta("stream", "ok"));
                        if (usage.get() != null) logTokenUsage(usage.get());
                    })
                    .doOnError(e -> {
                        String resultado = e instanceof TimeoutException ? "timeout" : "error";
                        muestra.stop(timerRespuesta("stream", resultado));
                        log.error("Chatbot (stream) falló [{}]: {}", resultado, e.getMessage());
                    })
                    .doOnCancel(() -> muestra.stop(timerRespuesta("stream", "cancelado")));
        });
    }

    private ChatClient.ChatClientRequestSpec prompt(Usuario usuario, String mensaje, boolean stream) {
        OpenAiChatOptions.Builder opciones = OpenAiChatOptions.builder()
                .toolNames("buscarContratosSecop")
                .toolContext(Map.of(TOOL_CONTEXT_USUARIO, usuario));
        if (stream) {
            // Sin esto OpenAI no envía el conteo de tokens en las respuestas por streaming.
            opciones.streamUsage(true);
        }
        return chatClient.prompt()
                .user(mensaje)
                .options(opciones.build());
    }

    private static String textoDe(ChatResponse response) {
        if (response.getResult() == null || response.getResult().getOutput() == null) return null;
        return response.getResult().getOutput().getText();
    }

    /** OpenAI solo manda el conteo en el último fragmento de cada ronda; el resto llega en cero. */
    private static void guardarUsage(ChatResponse response, AtomicReference<Usage> usage) {
        if (response.getMetadata() == null) return;
        Usage actual = response.getMetadata().getUsage();
        if (actual != null && actual.getTotalTokens() != null && actual.getTotalTokens() > 0) {
            usage.set(actual);
        }
    }

    private Timer timerRespuesta(String modo, String resultado) {
        return Timer.builder("chatbot.respuesta")
                .description("Tiempo total de respuesta del chatbot")
                .tag("modo", modo)
                .tag("resultado", resultado)
                .register(meterRegistry);
    }

    /**
     * Registra el consumo de tokens de la interacción para poder reportar rendimiento/costo.
     *
     * <p>Solo se loguean conteos numéricos (prompt/completion/total). El mensaje del usuario
     * y la respuesta del modelo nunca se incluyen aquí: los usuarios consultan contratos
     * reales a través del chatbot y este log no debe convertirse en un registro de esas
     * conversaciones.</p>
     */
    private void logTokenUsage(Usage usage) {
        log.info("Chatbot tokens - prompt={}, completion={}, total={}",
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }
}
