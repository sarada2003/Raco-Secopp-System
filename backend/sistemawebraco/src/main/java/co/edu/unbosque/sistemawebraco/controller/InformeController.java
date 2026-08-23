package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.dto.InformeDetalleDTO;
import co.edu.unbosque.sistemawebraco.dto.InformeResumenDTO;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.services.InformeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/informes")
@Tag(name = "Informes", description = "Historial automático de búsquedas de contratos SECOP II (filtro manual y chatbot)")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "Bearer JWT")
public class InformeController {

    private final InformeService informeService;
    private final UsuarioRepository usuarioRepository;

    public InformeController(InformeService informeService, UsuarioRepository usuarioRepository) {
        this.informeService = informeService;
        this.usuarioRepository = usuarioRepository;
    }

    @Operation(summary = "Listar el historial de búsquedas del usuario autenticado",
            description = "Cada entrada se guardó sola al ejecutarse una búsqueda en /api/secop/buscar " +
                          "o desde el chatbot — no hay una acción manual de \"crear informe\".")
    @GetMapping
    public List<InformeResumenDTO> listar(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        return informeService.listar(usuario.getId());
    }

    @Operation(summary = "Obtener el detalle completo de una entrada del historial",
            description = "Incluye el criterio usado y el snapshot de resultados — lo que necesita el frontend " +
                          "para renderizar la tabla y exportar el PDF. Solo el dueño puede consultarlo (404 si no lo es).")
    @GetMapping("/{id}")
    public InformeDetalleDTO obtener(@PathVariable Long id, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        return informeService.obtener(id, usuario.getId());
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }
}
