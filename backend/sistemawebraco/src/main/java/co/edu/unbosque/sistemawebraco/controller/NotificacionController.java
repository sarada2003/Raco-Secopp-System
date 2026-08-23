package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.dto.NotificacionDTO;
import co.edu.unbosque.sistemawebraco.dto.SuscripcionDTO;
import co.edu.unbosque.sistemawebraco.dto.SuscripcionRequestDTO;
import co.edu.unbosque.sistemawebraco.entity.SuscripcionAlerta;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.services.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Alertas de contratos SECOP II y suscripciones recurrentes del usuario autenticado")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "Bearer JWT")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;

    public NotificacionController(NotificacionService notificacionService, UsuarioRepository usuarioRepository) {
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
    }

    @Operation(summary = "Listar notificaciones del usuario autenticado (no leídas primero)")
    @GetMapping
    public List<NotificacionDTO> listar(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        return notificacionService.listar(usuario.getId());
    }

    @Operation(summary = "Cantidad de notificaciones no leídas (para el contador del ícono de campana)")
    @GetMapping("/no-leidas/count")
    public Map<String, Long> contarNoLeidas(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        return Map.of("count", notificacionService.contarNoLeidas(usuario.getId()));
    }

    @Operation(summary = "Marcar una notificación como leída",
            description = "Solo el dueño de la notificación puede marcarla — un id de otro usuario responde 404.")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        notificacionService.marcarLeida(id, usuario.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Marcar todas las notificaciones del usuario como leídas")
    @PatchMapping("/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        notificacionService.marcarTodasLeidas(usuario.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Guardar un criterio de búsqueda como alerta recurrente",
            description = "Reutiliza el mismo formato de filtros de /api/secop/buscar. Si ya existe una " +
                          "suscripción activa idéntica, la devuelve en vez de crear un duplicado.")
    @PostMapping("/suscripciones")
    public ResponseEntity<SuscripcionAlerta> crearSuscripcion(
            @RequestBody SuscripcionRequestDTO request, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        SuscripcionAlerta suscripcion = notificacionService.crearSuscripcion(usuario, request);
        return ResponseEntity.ok(suscripcion);
    }

    @Operation(summary = "Listar las alertas recurrentes activas del usuario autenticado")
    @GetMapping("/suscripciones")
    public List<SuscripcionDTO> listarSuscripciones(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        return notificacionService.listarSuscripciones(usuario.getId());
    }

    @Operation(summary = "Desactivar una alerta recurrente",
            description = "Solo el dueño de la suscripción puede desactivarla — un id de otro usuario responde 404.")
    @DeleteMapping("/suscripciones/{id}")
    public ResponseEntity<Void> eliminarSuscripcion(@PathVariable Long id, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        notificacionService.eliminarSuscripcion(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }
}
