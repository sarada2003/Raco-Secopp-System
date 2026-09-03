package co.edu.unbosque.sistemawebraco.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilidades compartidas por los benchmarks de rendimiento (paquete {@code perf}, tag JUnit
 * "perf" — excluidos del suite por defecto en build.gradle).
 *
 * <p>A diferencia de los tests unitarios/de seguridad, estos benchmarks NO levantan un
 * contexto de Spring propio: pegan por HTTP puro a un backend YA CORRIENDO (el mismo que
 * arrancarías con {@code ./gradlew bootRun}), con sus credenciales reales de SECOP/OpenAI.
 * Así se evita cualquier conflicto con el perfil H2 + secretos dummy de
 * {@code src/test/resources/application.properties} (pensado para el suite normal, no para
 * medir contra las APIs externas reales) y se miden tiempos de extremo a extremo tal como
 * los percibiría el frontend.</p>
 */
final class PerfSupport {

    static final String BASE_URL = System.getProperty("raco.perf.baseUrl", "http://localhost:8090");
    private static final String EMAIL = System.getProperty("raco.perf.email", "perf-bench@raco.local");
    private static final String PASSWORD = System.getProperty("raco.perf.password", "PerfBench123");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PerfSupport() {}

    /**
     * Aborta el test (en vez de fallarlo) si el backend no está corriendo en BASE_URL.
     *
     * <p>Nota: {@code /actuator/health} no está en la lista {@code permitAll} de
     * {@code SecurityConfig} (deliberado — no forma parte del alcance de rutas públicas
     * pactado), así que sin token responde 401, no 200. Eso sigue siendo prueba de que el
     * backend está arriba y su cadena de filtros de seguridad responde; cualquier respuesta
     * HTTP recibida (incluido un 401) cuenta como "disponible". Solo una excepción real de
     * conexión (backend apagado) aborta el benchmark.</p>
     */
    static void asumirBackendDisponible() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/actuator/health"))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            Assumptions.abort("No se pudo conectar al backend en " + BASE_URL +
                    ". Arráncalo con './gradlew bootRun' (con tus variables de entorno reales) antes de correr los benchmarks: " + e.getMessage());
        }
    }

    /** Registra (ignora 400 si ya existe) y hace login del usuario de benchmark; devuelve el JWT. */
    static String obtenerToken() throws IOException, InterruptedException {
        String registroBody = MAPPER.writeValueAsString(mapaDe(
                "nombre", "Perf Bench",
                "email", EMAIL,
                "password", PASSWORD));
        post("/api/auth/register", registroBody, null); // 200 si es la primera vez, 400 si ya existe — ambos ok aquí

        String loginBody = MAPPER.writeValueAsString(mapaDe("email", EMAIL, "password", PASSWORD));
        HttpResponse<String> loginResponse = post("/api/auth/login", loginBody, null);
        if (loginResponse.statusCode() != 200) {
            throw new IllegalStateException("No se pudo autenticar el usuario de benchmark (" + EMAIL + "): " +
                    loginResponse.statusCode() + " " + loginResponse.body());
        }
        JsonNode json = MAPPER.readTree(loginResponse.body());
        return json.get("token").asText();
    }

    static HttpResponse<String> post(String path, String jsonBody, String bearerToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(100))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (bearerToken != null) builder.header("Authorization", "Bearer " + bearerToken);
        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Corre {@code llamada} {@code iteraciones} veces y devuelve los tiempos en milisegundos de cada corrida. */
    static List<Long> medirTiempos(int iteraciones, Ejecutable llamada) throws Exception {
        return java.util.stream.IntStream.range(0, iteraciones).mapToObj(i -> {
            try {
                long inicio = System.nanoTime();
                llamada.ejecutar();
                return (System.nanoTime() - inicio) / 1_000_000;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    static String resumen(String etiqueta, List<Long> tiemposMs) {
        double promedio = tiemposMs.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = tiemposMs.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = tiemposMs.stream().mapToLong(Long::longValue).max().orElse(0);
        List<Long> ordenado = tiemposMs.stream().sorted().collect(Collectors.toList());
        long p95 = ordenado.get((int) Math.ceil(ordenado.size() * 0.95) - 1);
        return "| %s | %d | %.0f | %d | %d | %d |".formatted(etiqueta, tiemposMs.size(), promedio, min, p95, max);
    }

    /**
     * Igual que {@link #resumen}, pero agrega una columna de tokens marcada explícitamente
     * como pendiente — el endpoint HTTP no expone el conteo de tokens (ver
     * {@link ChatbotPerformanceTest}), así que no se debe dejar esa columna vacía ni con un
     * placeholder ambiguo.
     */
    static String resumenConTokensPendientes(String etiqueta, List<Long> tiemposMs) {
        return resumen(etiqueta, tiemposMs) + " Pendiente (ver log del backend) |";
    }

    static void escribirReporte(String archivo, String encabezado, List<String> filas) throws IOException {
        escribirReporte(archivo, encabezado, filas, false);
    }

    static void escribirReporte(String archivo, String encabezado, List<String> filas, boolean incluyeColumnaTokens) throws IOException {
        Path dir = Path.of("build", "perf-results");
        Files.createDirectories(dir);
        StringBuilder sb = new StringBuilder();
        sb.append(encabezado).append("\n");
        sb.append("| Escenario | Muestras | Promedio (ms) | Mín (ms) | P95 (ms) | Máx (ms) |")
                .append(incluyeColumnaTokens ? " Tokens (prompt/completion/total) |\n" : "\n");
        sb.append("|---|---|---|---|---|---|").append(incluyeColumnaTokens ? "---|\n" : "\n");
        filas.forEach(f -> sb.append(f).append("\n"));
        Path destino = dir.resolve(archivo);
        Files.writeString(destino, sb.toString());
        System.out.println("Reporte de rendimiento escrito en: " + destino.toAbsolutePath());
        System.out.println(sb);
    }

    private static java.util.Map<String, String> mapaDe(String... kv) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put(kv[i], kv[i + 1]);
        return map;
    }

    @FunctionalInterface
    interface Ejecutable {
        void ejecutar() throws Exception;
    }
}
