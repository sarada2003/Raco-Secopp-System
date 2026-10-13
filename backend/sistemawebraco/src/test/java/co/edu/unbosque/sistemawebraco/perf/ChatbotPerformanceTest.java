package co.edu.unbosque.sistemawebraco.perf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark manual del tiempo de respuesta de POST /api/chatbot/chat y de su variante en
 * streaming POST /api/chatbot/chat/stream (tiempo hasta el primer fragmento y hasta el final),
 * que internamente llaman a gpt-4o-mini (con o sin tool-call a SECOP II). Igual que
 * {@link SecopSearchPerformanceTest}, requiere el backend corriendo con credenciales reales
 * (OPENAI_API_KEY) y no forma parte del suite por defecto.
 *
 * <p>Corre pocas iteraciones a propósito: cada llamada es una petición real y facturable a
 * OpenAI. El <b>conteo de tokens</b> por interacción no se puede leer desde este test (el
 * endpoint solo devuelve el texto de la respuesta) — {@link co.edu.unbosque.sistemawebraco.services.ChatbotService}
 * lo loguea en el servidor en cada llamada (línea {@code "Chatbot tokens - prompt=..., completion=..., total=..."});
 * revisa la consola/log del backend mientras corres este benchmark para completar esa columna
 * del reporte de rendimiento.</p>
 *
 * <p><b>Cómo correrlo:</b> {@code ./gradlew test -PincludePerf --tests "ChatbotPerformanceTest"}
 * con el backend arrancado aparte ({@code ./gradlew bootRun}).</p>
 */
@Tag("perf")
class ChatbotPerformanceTest {

    private static String token;
    private static final int ITERACIONES = 3;

    @BeforeAll
    static void setUp() throws Exception {
        PerfSupport.asumirBackendDisponible();
        token = PerfSupport.obtenerToken();
    }

    private static final String[][] MENSAJES = {
            {"Pregunta general (sin tool-call)",
                    "¿Qué es SECOP II y para qué sirve?"},
            {"Búsqueda simple (con tool-call a SECOP)",
                    "Busca contratos de consultoría en tecnología"},
            {"Búsqueda con varios filtros (con tool-call a SECOP)",
                    "Muéstrame contratos de obras de la Alcaldía de Bogotá por más de 50 millones de pesos"},
    };

    @Test
    void mideTiempoDeRespuestaDeMensajesRepresentativos() throws Exception {
        List<String> filas = new ArrayList<>();

        for (String[] m : MENSAJES) {
            filas.add(medirMensaje(m[0] + " — /chat (respuesta completa)", m[1]));
            filas.addAll(medirMensajeStream(m[0], m[1]));
        }

        PerfSupport.escribirReporte("chatbot-performance.md",
                "# Rendimiento — chatbot (" + ITERACIONES + " muestras por mensaje y endpoint)\n\n" +
                "> `/chat` devuelve la respuesta en un solo bloque; `/chat/stream` la envía por fragmentos (SSE). " +
                "\"Primer fragmento\" es lo que el usuario espera antes de ver texto en pantalla.\n>\n" +
                "> Columna \"Tokens\": el endpoint no expone el conteo por HTTP, así que queda marcada como " +
                "pendiente a propósito. Revisa el log del backend (\"Chatbot tokens - prompt=..., completion=..., " +
                "total=...\") durante esta corrida y complétala a mano antes de citar este reporte en la tesis. " +
                "El desglose por fase está en /actuator/metrics (chatbot.respuesta, chatbot.primer.fragmento, " +
                "chatbot.herramienta.secop).",
                filas, true);
    }

    private List<String> medirMensajeStream(String etiqueta, String mensaje) throws Exception {
        String body = "{\"mensaje\":\"" + mensaje.replace("\"", "\\\"") + "\"}";
        List<Long> primerFragmento = new ArrayList<>();
        List<Long> total = new ArrayList<>();
        for (int i = 0; i < ITERACIONES; i++) {
            long[] tiempos = PerfSupport.postStream("/api/chatbot/chat/stream", body, token);
            primerFragmento.add(tiempos[0]);
            total.add(tiempos[1]);
        }
        return List.of(
                PerfSupport.resumenConTokensPendientes(etiqueta + " — /chat/stream primer fragmento", primerFragmento),
                PerfSupport.resumenConTokensPendientes(etiqueta + " — /chat/stream respuesta completa", total));
    }

    private String medirMensaje(String etiqueta, String mensaje) throws Exception {
        String body = "{\"mensaje\":\"" + mensaje.replace("\"", "\\\"") + "\"}";
        List<Long> tiempos = PerfSupport.medirTiempos(ITERACIONES, () -> {
            var response = PerfSupport.post("/api/chatbot/chat", body, token);
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Respuesta inesperada (" + response.statusCode() + "): " + response.body());
            }
        });
        return PerfSupport.resumenConTokensPendientes(etiqueta, tiempos);
    }
}
