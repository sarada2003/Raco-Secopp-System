package co.edu.unbosque.sistemawebraco.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos públicos del usuario del sistema (nunca incluye la contraseña hasheada)")
public class UsuarioDTO {

    @Schema(description = "ID único del usuario generado por la base de datos", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    @Schema(description = "Correo electrónico único del usuario", example = "juan@correo.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Indica si la cuenta está activa", example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean activo;

    @Schema(description = "Fecha y hora de registro en el sistema", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaRegistro;

    @Schema(description = "Contraseña en texto plano (solo en registro, nunca se retorna en respuestas)",
            example = "MiPassword1",
            accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
