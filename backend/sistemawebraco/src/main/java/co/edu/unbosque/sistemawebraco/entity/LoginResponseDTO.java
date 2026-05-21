package co.edu.unbosque.sistemawebraco.entity;

import co.edu.unbosque.sistemawebraco.dto.UsuarioDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta devuelta al iniciar sesión correctamente")
public class LoginResponseDTO {

    @Schema(description = "Token JWT firmado con HS512, válido por 24 horas",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c3VhcmlvQGNvcnJlby5jb20iLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDA4NjQwMH0.xxx")
    private String token;

    @Schema(description = "Datos públicos del usuario autenticado (sin contraseña)")
    private UsuarioDTO usuario;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UsuarioDTO getUsuario() { return usuario; }
    public void setUsuario(UsuarioDTO usuario) { this.usuario = usuario; }
}
