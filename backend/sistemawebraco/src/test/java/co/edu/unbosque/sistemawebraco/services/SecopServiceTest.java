package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecopService nunca debe pegarle a datos.gov.co en los tests: en vez de mockear la cadena
 * fluida de WebClient con Mockito (frágil, muchos métodos encadenados), se reemplaza la
 * {@code exchangeFunction} interna por un stub que captura la URI construida y devuelve una
 * respuesta enlatada — la técnica recomendada por la propia documentación de Spring WebFlux
 * para testear clientes basados en WebClient sin red real.
 */
class SecopServiceTest {

    private static final String RESOURCE_PATH = "/resource/p6dx-8zbt.json";
    private static final String CONTRATO_JSON = """
            [{"id_del_proceso":"CO1.PCCNTR.1","entidad":"Alcaldía de Bogotá",
              "nombre_del_procedimiento":"Consultoría técnica","valor_total_adjudicacion":100000000}]
            """;

    private final AtomicReference<URI> uriCapturado = new AtomicReference<>();

    private SecopService construirServicio(int statusCode, String body) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(stubExchangeFunction(statusCode, body))
                .build();

        SecopService secopService = new SecopService(webClient);
        ReflectionTestUtils.setField(secopService, "resourcePath", RESOURCE_PATH);
        return secopService;
    }

    private ExchangeFunction stubExchangeFunction(int statusCode, String body) {
        return (ClientRequest request) -> {
            uriCapturado.set(request.url());
            return Mono.just(ClientResponse.create(HttpStatus.valueOf(statusCode))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        };
    }

    @Test
    void buscarContratosDevuelveLosResultadosDeserializados() {
        SecopService secopService = construirServicio(200, CONTRATO_JSON);

        List<SecopContratoDTO> resultado = secopService.buscarContratos(new ContractSearchRequest());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdProceso()).isEqualTo("CO1.PCCNTR.1");
        assertThat(resultado.get(0).getEntidad()).isEqualTo("Alcaldía de Bogotá");
        assertThat(resultado.get(0).getValor()).isEqualByComparingTo("100000000");
    }

    @Test
    void combinaPalabraClaveYEntidadEnElParametroQ() {
        SecopService secopService = construirServicio(200, CONTRATO_JSON);
        ContractSearchRequest request = new ContractSearchRequest();
        request.setPalabraClave("consultoría");
        request.setEntidad("Ministerio");
        request.setPage(0);
        request.setSize(20);

        secopService.buscarContratos(request);

        String decoded = java.net.URLDecoder.decode(uriCapturado.get().getRawQuery(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decoded).contains("$q=consultoría Ministerio");
    }

    @Test
    void sinPalabraClaveNiEntidadNoEnviaParametroQ() {
        SecopService secopService = construirServicio(200, CONTRATO_JSON);

        secopService.buscarContratos(new ContractSearchRequest());

        assertThat(uriCapturado.get().getRawQuery()).doesNotContain("%24q=");
    }

    @Test
    void filtrosDeIgualdadYRangoArmanElWhereClause() {
        SecopService secopService = construirServicio(200, CONTRATO_JSON);
        ContractSearchRequest request = new ContractSearchRequest();
        request.setDepartamento("Cundinamarca");
        request.setValorMin(new BigDecimal("1000000"));
        request.setValorMax(new BigDecimal("50000000"));
        request.setPage(0);
        request.setSize(10);

        secopService.buscarContratos(request);

        String decoded = java.net.URLDecoder.decode(uriCapturado.get().getRawQuery(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decoded).contains("departamento_entidad = 'Cundinamarca'");
        assertThat(decoded).contains("valor_total_adjudicacion >= 1000000");
        assertThat(decoded).contains("valor_total_adjudicacion <= 50000000");
    }

    @Test
    void paginacionCalculaOffsetComoPaginaPorTamano() {
        SecopService secopService = construirServicio(200, CONTRATO_JSON);
        ContractSearchRequest request = new ContractSearchRequest();
        request.setPage(3);
        request.setSize(20);

        secopService.buscarContratos(request);

        String decoded = java.net.URLDecoder.decode(uriCapturado.get().getRawQuery(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decoded).contains("$offset=60"); // page 3 * size 20
        assertThat(decoded).contains("$limit=20");
    }

    @Test
    void ordenarPorValorMayorUsaOrdenDescendentePorValor() {
        SecopService secopService = construirServicio(200, CONTRATO_JSON);
        ContractSearchRequest request = new ContractSearchRequest();
        request.setOrdenarPor("valor_mayor");
        request.setPage(0);
        request.setSize(10);

        secopService.buscarContratos(request);

        String decoded = java.net.URLDecoder.decode(uriCapturado.get().getRawQuery(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decoded).contains("$order=valor_total_adjudicacion DESC");
    }

    @Test
    void sinOrdenarPorUsaFechaDescendentePorDefecto() {
        SecopService secopService = construirServicio(200, CONTRATO_JSON);

        secopService.buscarContratos(new ContractSearchRequest());

        String decoded = java.net.URLDecoder.decode(uriCapturado.get().getRawQuery(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decoded).contains("$order=fecha_de_publicacion_del DESC");
    }

    @Test
    void rangoDeFechasSeConvierteAFormatoIsoConHoraFijaDeInicioYFin() {
        SecopService secopService = construirServicio(200, CONTRATO_JSON);
        ContractSearchRequest request = new ContractSearchRequest();
        request.setFechaFirmaDesde(LocalDate.of(2024, 1, 1));
        request.setFechaFirmaHasta(LocalDate.of(2024, 12, 31));
        request.setPage(0);
        request.setSize(10);

        secopService.buscarContratos(request);

        String decoded = java.net.URLDecoder.decode(uriCapturado.get().getRawQuery(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decoded).contains("fecha_de_publicacion_del >= '2024-01-01T00:00:00'");
        assertThat(decoded).contains("fecha_de_publicacion_del <= '2024-12-31T23:59:59'");
    }

    @Test
    void consultarSecopCalculaOffsetConPaginaBase1() {
        SecopService secopService = construirServicio(200, CONTRATO_JSON);

        secopService.consultarSecop(2, 10);

        String decoded = java.net.URLDecoder.decode(uriCapturado.get().getRawQuery(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decoded).contains("$offset=10"); // (page 2 - 1) * size 10
    }

    @Test
    void respuestaNoExitosaSeConvierteEnErrorDeSecop() {
        SecopService secopService = construirServicio(500, "{\"error\":\"boom\"}");

        assertThatThrownBy(() -> secopService.buscarContratos(new ContractSearchRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }
}
