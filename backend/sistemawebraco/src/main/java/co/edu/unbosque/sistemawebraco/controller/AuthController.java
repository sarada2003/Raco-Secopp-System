package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.dto.UsuarioDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginRequestDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginResponseDTO;
import co.edu.unbosque.sistemawebraco.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Operaciones relacionadas con el inicio de sesión de usuarios")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(
            summary = "Iniciar sesión",
            description = "Permite a un usuario autenticarse con su nombre de usuario y contraseña. Retorna un token JWT si las credenciales son válidas.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
            }
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/register")
    public ResponseEntity<UsuarioDTO> register(@RequestBody UsuarioDTO dto) {
        UsuarioDTO registrado = authService.register(dto);
        return ResponseEntity.ok(registrado);
    }



    @GetMapping("/me")
    @Operation(summary = "Obtener datos del usuario autenticado")
    public ResponseEntity<UsuarioDTO> getUsuarioActual(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        UsuarioDTO usuario = authService.obtenerUsuarioDesdeToken(token);
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/google-login")
    @Operation(summary = "Iniciar sesión con Google usando el email")
    public ResponseEntity<LoginResponseDTO> loginConGoogle(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        LoginResponseDTO response = authService.loginConGoogle(email);
        return ResponseEntity.ok(response);
    }




}
