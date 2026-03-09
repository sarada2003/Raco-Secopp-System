package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.query.SoqlQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecopService {

    private final WebClient webClient;

    @Value("${secop.api.resource-path}")
    private String resourcePath;

    @Value("${secop.api.app-token}")
    private String appToken;


    public List<SecopContratoDTO> consultarSecop(int page, int size) {

        int offset = (page - 1) * size;

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(resourcePath)
                        .queryParam("$limit", size)
                        .queryParam("$offset", offset)
                        .build())
                .retrieve()
                .bodyToFlux(SecopContratoDTO.class)
                .collectList()
                .block();
    }
    public List<SecopContratoDTO> buscarContratos(ContractSearchRequest request) {

        int offset = request.getPage() * request.getSize();

        StringBuilder where = new StringBuilder();

        // filtro por valor
        if (request.getValorMin() != null) {
            where.append("valor_total_adjudicacion>")
                    .append(request.getValorMin());
        }

        // filtro por palabra clave
        if (request.getPalabraClave() != null && !request.getPalabraClave().isBlank()) {

            if (where.length() > 0) {
                where.append(" AND ");
            }

            where.append("descripci_n_del_procedimiento like '%")
                    .append(request.getPalabraClave())
                    .append("%'");
        }

        return webClient.get()
                .uri(uriBuilder -> {

                    uriBuilder.path(resourcePath);

                    if (where.length() > 0) {
                        uriBuilder.queryParam("$where", where.toString());
                    }

                    uriBuilder.queryParam("$limit", request.getSize());
                    uriBuilder.queryParam("$offset", offset);

                    var uri = uriBuilder.build();

                    System.out.println("SECOP URL -> " + uri.toString());

                    return uri;
                })
                .retrieve()
                .bodyToFlux(SecopContratoDTO.class)
                .collectList()
                .block();
    }
}


