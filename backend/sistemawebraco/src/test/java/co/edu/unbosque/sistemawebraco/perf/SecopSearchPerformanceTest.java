package co.edu.unbosque.sistemawebraco.perf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark manual del tiempo de respuesta de POST /api/secop/buscar (que a su vez pega a
 * datos.gov.co en tiempo real). No corre en {@code ./gradlew test}: requiere el backend
 * arrancado con credenciales reales (SECOP_APP_TOKEN) y depende de la disponibilidad de la
 * API externa, así que no es apto para un suite que deba ser determinista/offline.
 *
 * <p><b>Cómo correrlo:</b>
 * <ol>
 *   <li>Arranca el backend con tus variables de entorno reales: {@code ./gradlew bootRun}</li>
 *   <li>En otra terminal: {@code ./gradlew test -PincludePerf --tests "SecopSearchPerformanceTest"}</li>
 * </ol>
 * El resultado queda en {@code build/perf-results/secop-search-performance.md}, listo para
 * pegar en el "Reporte de pruebas de rendimiento" de la tesis.</p>
 */
@Tag("perf")
class SecopSearchPerformanceTest {

    private static String token;
    private static final int ITERACIONES = 10;

    @BeforeAll
    static void setUp() throws Exception {
        PerfSupport.asumirBackendDisponible();
        token = PerfSupport.obtenerToken();
    }

    @Test
    void mideTiempoDeRespuestaDeLosEscenariosDeBusquedaMasComunes() throws Exception {
        List<String> filas = new ArrayList<>();

        filas.add(medirEscenario("Sin filtros (listado general)",
                "{\"page\":0,\"size\":10}"));
        filas.add(medirEscenario("Por palabra clave",
                "{\"palabraClave\":\"consultoría\",\"page\":0,\"size\":10}"));
        filas.add(medirEscenario("Por rango de valor",
                "{\"valorMin\":50000000,\"valorMax\":500000000,\"page\":0,\"size\":10}"));
        filas.add(medirEscenario("Combinada (palabra clave + entidad + valor)",
                "{\"palabraClave\":\"obras\",\"entidad\":\"Alcaldía\",\"valorMin\":10000000,\"page\":0,\"size\":10}"));

        PerfSupport.escribirReporte("secop-search-performance.md",
                "# Rendimiento — POST /api/secop/buscar (" + ITERACIONES + " muestras por escenario)",
                filas);
    }

    private String medirEscenario(String etiqueta, String body) throws Exception {
        List<Long> tiempos = PerfSupport.medirTiempos(ITERACIONES, () -> {
            var response = PerfSupport.post("/api/secop/buscar", body, token);
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Respuesta inesperada (" + response.statusCode() + "): " + response.body());
            }
        });
        return PerfSupport.resumen(etiqueta, tiempos);
    }
}
