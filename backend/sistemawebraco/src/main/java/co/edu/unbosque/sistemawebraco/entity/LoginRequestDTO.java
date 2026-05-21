package co.edu.unbosque.sistemawebraco.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Credenciales requeridas para iniciar sesión")
public class LoginRequestDTO {

    @Schema(description = "Correo electrónico del usuario", example = "usuario@correo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Contraseña del usuario", example = "MiPassword1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
