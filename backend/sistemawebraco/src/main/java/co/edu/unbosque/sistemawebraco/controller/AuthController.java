package co.edu.unbosque.sistemawebraco.controller;

import co.edu.unbosque.sistemawebraco.dto.UsuarioDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginRequestDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginResponseDTO;
import co.edu.unbosque.sistemawebraco.services.AuthService;
import co.edu.unbosque.sistemawebraco.services.SecopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, login y consulta de usuario autenticado")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private SecopService secopService;

    // -------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario con email y contraseña. " +
                          "Retorna un **token JWT** (válido 24h) que debe enviarse en el header " +
                          "`Authorization: Bearer {token}` en todas las peticiones protegidas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso — retorna token y datos del usuario",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Contraseña incorrecta", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado con ese email", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Credenciales del usuario",
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "Ejemplo de login",
                            value = "{\"email\": \"usuario@correo.com\", \"password\": \"MiPassword1\"}"
                    ))
            )
            @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // POST /api/auth/register
    // -------------------------------------------------------
    @Operation(
            summary = "Registrar nuevo usuario",
            description = "Crea una nueva cuenta de usuario. La contraseña se encripta con **BCrypt** antes de guardarse. " +
                          "El campo `password` es de solo escritura y **nunca se retorna** en la respuesta."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "400", description = "El email ya está registrado en el sistema", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<UsuarioDTO> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del nuevo usuario",
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "Ejemplo de registro",
                            value = "{\"nombre\": \"Juan Pérez\", \"email\": \"juan@correo.com\", \"password\": \"MiPassword1\"}"
                    ))
            )
            @RequestBody UsuarioDTO dto) {
        UsuarioDTO registrado = authService.register(dto);
        return ResponseEntity.ok(registrado);
    }

    // -------------------------------------------------------
    // GET /api/auth/me
    // -------------------------------------------------------
    @Operation(
            summary = "Obtener usuario autenticado",
            description = "Retorna los datos del usuario correspondiente al token JWT enviado en el header `Authorization`. " +
                          "Útil para cargar el perfil al iniciar la sesión en el frontend.",
            security = @SecurityRequirement(name = "Bearer JWT")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario autenticado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido o expirado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario del token no encontrado en la base de datos", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> getUsuarioActual(
            @Parameter(hidden = true) HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        UsuarioDTO usuario = authService.obtenerUsuarioDesdeToken(token);
        return ResponseEntity.ok(usuario);
    }

    // -------------------------------------------------------
    // POST /api/auth/google-login
    // -------------------------------------------------------
    @Operation(
            summary = "Iniciar sesión con Google (OAuth2)",
            description = "Recibe el email verificado por Firebase Auth (login con Google) y genera un token JWT " +
                          "del sistema RACO para el usuario. El usuario debe estar previamente registrado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token JWT generado correctamente para el usuario de Google",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "El email de Google no está registrado en el sistema", content = @Content)
    })
    @PostMapping("/google-login")
    public ResponseEntity<LoginResponseDTO> loginConGoogle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Email verificado por Google/Firebase",
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "Ejemplo",
                            value = "{\"email\": \"juan@gmail.com\"}"
                    ))
            )
            @RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        LoginResponseDTO response = authService.loginConGoogle(email);
        return ResponseEntity.ok(response);
    }
}
