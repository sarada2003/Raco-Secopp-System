package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.services.SecopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/secop")
@Tag(name = "SECOP II", description = "Consulta y filtrado de contratos públicos desde el sistema SECOP II de Colombia Compra Eficiente")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "Bearer JWT")
public class SecopController {

    private final SecopService secopService;

    public SecopController(SecopService secopService) {
        this.secopService = secopService;
    }

    // -------------------------------------------------------
    // GET /api/secop/contratos
    // -------------------------------------------------------
    @Operation(
            summary = "Listar contratos paginados",
            description = "Retorna una lista paginada de contratos públicos obtenidos directamente desde la API de **datos.gov.co** " +
                          "(SECOP II). No aplica filtros — útil para explorar el dataset completo. " +
                          "Requiere token JWT en el header `Authorization`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Lista de contratos obtenida exitosamente",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = SecopContratoDTO.class)))),
            @ApiResponse(responseCode = "401",
                    description = "Token JWT ausente, inválido o expirado",
                    content = @Content),
            @ApiResponse(responseCode = "502",
                    description = "Error al consultar la API externa de datos.gov.co",
                    content = @Content)
    })
    @GetMapping("/contratos")
    public ResponseEntity<List<SecopContratoDTO>> obtenerContratos(
            @Parameter(description = "Número de página (base 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Cantidad de registros por página (máx. recomendado: 20)", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        List<SecopContratoDTO> contratos = secopService.consultarSecop(page, size);
        return ResponseEntity.ok(contratos);
    }

    // -------------------------------------------------------
    // POST /api/secop/buscar
    // -------------------------------------------------------
    @Operation(
            summary = "Buscar contratos con filtros",
            description = "Filtra contratos de SECOP II según los criterios enviados en el cuerpo de la petición. " +
                          "Todos los campos son opcionales — los campos nulos o vacíos se ignoran. " +
                          "Los filtros disponibles son: **palabra clave** en la descripción, **entidad** contratante y **valor mínimo** de adjudicación. " +
                          "Soporta paginación mediante `page` (base 0) y `size`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Contratos que coinciden con los filtros aplicados",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = SecopContratoDTO.class)))),
            @ApiResponse(responseCode = "400",
                    description = "Cuerpo de la petición inválido",
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Token JWT ausente, inválido o expirado",
                    content = @Content),
            @ApiResponse(responseCode = "502",
                    description = "Error al consultar la API externa de datos.gov.co",
                    content = @Content)
    })
    @PostMapping("/buscar")
    public List<SecopContratoDTO> buscar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Filtros de búsqueda (todos opcionales)",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ContractSearchRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Buscar por palabra clave",
                                            value = "{\"palabraClave\": \"consultoría\", \"page\": 0, \"size\": 10}"
                                    ),
                                    @ExampleObject(
                                            name = "Buscar por valor mínimo",
                                            value = "{\"valorMin\": 100000000, \"page\": 0, \"size\": 5}"
                                    ),
                                    @ExampleObject(
                                            name = "Búsqueda combinada",
                                            value = "{\"palabraClave\": \"obras\", \"valorMin\": 50000000, \"page\": 0, \"size\": 10}"
                                    )
                            }
                    )
            )
            @RequestBody ContractSearchRequest request) {
        return secopService.buscarContratos(request);
    }
}
