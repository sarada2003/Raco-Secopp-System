package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.query.SoqlQueryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecopService {

    private final WebClient webClient;

    @Value("${secop.api.resource-path}")
    private String resourcePath;

    // Campos reales del dataset p6dx-8zbt (SECOP II procesos de contratación)
    private static final String ORDER_FECHA_DESC  = "fecha_de_publicacion_del DESC";
    private static final String ORDER_FECHA_ASC   = "fecha_de_publicacion_del ASC";
    private static final String ORDER_VALOR_DESC  = "valor_total_adjudicacion DESC";
    private static final String ORDER_VALOR_ASC   = "valor_total_adjudicacion ASC";

    public List<SecopContratoDTO> consultarSecop(int page, int size) {
        int offset = (page - 1) * size;

        return webClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder
                            .path(resourcePath)
                            .queryParam("$order", ORDER_FECHA_DESC)
                            .queryParam("$limit", size)
                            .queryParam("$offset", offset)
                            .build();
                    log.info("SECOP consultarSecop → {}", uri);
                    return uri;
                })
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("SECOP error [{}]: {}", response.statusCode(), body))
                                .map(body -> new RuntimeException("SECOP [" + response.statusCode() + "]: " + body))
                )
                .bodyToFlux(SecopContratoDTO.class)
                .collectList()
                .timeout(Duration.ofSeconds(60))
                .onErrorMap(e -> {
                    log.error("SECOP consultarSecop falló: {}", e.getMessage());
                    return new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                            "La API de SECOP no respondió a tiempo", e);
                })
                .block();
    }

    public List<SecopContratoDTO> buscarContratos(ContractSearchRequest request) {
        int offset = request.getPage() * request.getSize();

        String orderClause = switch (request.getOrdenarPor() != null ? request.getOrdenarPor() : "") {
            case "valor_mayor"   -> ORDER_VALOR_DESC;
            case "valor_menor"   -> ORDER_VALOR_ASC;
            case "fecha_antigua" -> ORDER_FECHA_ASC;
            default              -> ORDER_FECHA_DESC;
        };

        // Combina palabraClave + entidad en $q (full-text indexado en Socrata).
        // Los campos de $where usan los nombres reales del dataset p6dx-8zbt.
        String fullTextQuery = Stream.of(request.getPalabraClave(), request.getEntidad())
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));

        SoqlQueryBuilder builder = new SoqlQueryBuilder()
                .fullText(fullTextQuery.isEmpty() ? null : fullTextQuery)
                .equals("departamento_entidad",       request.getDepartamento())
                .equals("modalidad_de_contratacion",  request.getModalidad())
                .equals("estado_del_procedimiento",   request.getEstado())
                .equals("tipo_de_contrato",           request.getTipoContrato())
                .min("valor_total_adjudicacion",      request.getValorMin())
                .max("valor_total_adjudicacion",      request.getValorMax())
                .dateBetween("fecha_de_publicacion_del",
                        request.getFechaFirmaDesde(), request.getFechaFirmaHasta());

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(resourcePath);
                    if (builder.getFullTextSearch() != null)
                        uriBuilder.queryParam("$q", builder.getFullTextSearch());
                    if (builder.hasConditions())
                        uriBuilder.queryParam("$where", builder.getWhereClause());
                    uriBuilder.queryParam("$order", orderClause);
                    uriBuilder.queryParam("$limit", request.getSize());
                    uriBuilder.queryParam("$offset", offset);
                    var uri = uriBuilder.build();
                    log.info("SECOP buscarContratos → {}", uri);
                    return uri;
                })
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("SECOP error [{}]: {}", response.statusCode(), body))
                                .map(body -> new RuntimeException("SECOP [" + response.statusCode() + "]: " + body))
                )
                .bodyToFlux(SecopContratoDTO.class)
                .collectList()
                .timeout(Duration.ofSeconds(60))
                .onErrorMap(e -> {
                    log.error("SECOP buscarContratos falló: {}", e.getMessage());
                    return new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                            "La API de SECOP no respondió a tiempo", e);
                })
                .block();
    }
}
