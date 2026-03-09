package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.services.SecopService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/secop")
@Tag(name = "SECOP", description = "Consultas a SECOP II")
@CrossOrigin(origins = "*")
public class SecopController {

    private final SecopService secopService;

    public SecopController(SecopService secopService) {
        this.secopService = secopService;
    }

    @GetMapping("/contratos")
    public ResponseEntity<List<SecopContratoDTO>> obtenerContratos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<SecopContratoDTO> contratos =
                secopService.consultarSecop(page, size);

        return ResponseEntity.ok(contratos);
    }

    @PostMapping("/buscar")
    public List<SecopContratoDTO> buscar(@RequestBody ContractSearchRequest request) {
        return secopService.buscarContratos(request);
    }
}
