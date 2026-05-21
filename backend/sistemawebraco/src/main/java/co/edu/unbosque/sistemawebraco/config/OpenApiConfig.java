package co.edu.unbosque.sistemawebraco.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RACO–SECOPP API")
                        .version("1.0.0")
                        .description("""
                                Backend del sistema **RACO–SECOPP**: consulta y filtrado de contratos públicos \
                                del sistema SECOP II de Colombia Compra Eficiente, autenticación de usuarios \
                                con JWT (HS512) y base para integración con chatbot IA.

                                ### Autenticación
                                La mayoría de endpoints requieren un token JWT. Obtenlo en `POST /api/auth/login` \
                                y luego haz clic en el botón **Authorize** (🔒) para ingresarlo.

                                ### Fuente de datos
                                Los contratos se consultan en tiempo real desde la API pública de \
                                [datos.gov.co](https://www.datos.gov.co).
                                """)
                        .contact(new Contact()
                                .name("Universidad El Bosque — Proyecto de Grado")
                                .email("proyecto@unbosque.edu.co"))
                        .license(new License()
                                .name("Uso académico")
                                .url("https://www.unbosque.edu.co")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8090")
                                .description("Servidor local de desarrollo")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .components(new Components()
                        .addSecuritySchemes("Bearer JWT", new SecurityScheme()
                                .name("Bearer JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en **POST /api/auth/login**. " +
                                             "Formato esperado en el header: `Authorization: Bearer {token}`")));
    }
}
