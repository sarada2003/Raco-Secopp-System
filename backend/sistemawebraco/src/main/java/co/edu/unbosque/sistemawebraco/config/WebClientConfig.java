package co.edu.unbosque.sistemawebraco.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración del cliente HTTP reactivo {@link WebClient} para consumir la API
 * pública de <b>SECOP II</b> en <a href="https://www.datos.gov.co">datos.gov.co</a>.
 *
 * <p>Centraliza en un único bean todos los parámetros de conexión a la API externa:
 * URL base y token de autenticación. Esto evita repetir la configuración en cada
 * clase que necesite hacer peticiones a SECOP.</p>
 *
 * <h3>Configuración requerida en {@code application.properties}</h3>
 * <pre>{@code
 * secop.api.base-url=https://www.datos.gov.co
 * secop.api.resource-path=/resource/p6dx-8zbt.json
 * secop.api.app-token=${SECOP_APP_TOKEN}
 * }</pre>
 *
 * <h3>Sobre el token de aplicación</h3>
 * <p>La API de Socrata (datos.gov.co) limita las peticiones anónimas a ~1000 por hora
 * por IP. Al registrar una aplicación en el portal y usar el {@code X-App-Token},
 * el límite sube significativamente. El token se inyecta desde la variable de entorno
 * {@code SECOP_APP_TOKEN}.</p>
 *
 * @author Proyecto de Grado — Universidad El Bosque
 * @see co.edu.unbosque.sistemawebraco.services.SecopService
 */
@Configuration
public class WebClientConfig {

    /**
     * URL base de la API de datos.gov.co.
     * Leída de {@code secop.api.base-url} en {@code application.properties}.
     * Ejemplo: {@code https://www.datos.gov.co}
     */
    @Value("${secop.api.base-url}")
    private String baseUrl;

    /**
     * Token de aplicación para la API de Socrata/datos.gov.co.
     * Leído de {@code secop.api.app-token}, que a su vez lee la variable de entorno
     * {@code SECOP_APP_TOKEN}.
     */
    @Value("${secop.api.app-token}")
    private String appToken;

    /**
     * Crea y configura el bean {@link WebClient} para consumir SECOP II.
     *
     * <p>Características del bean:</p>
     * <ul>
     *   <li><b>Base URL:</b> fijada en {@code secop.api.base-url} — todas las URIs
     *       relativas se resuelven contra esta base.</li>
     *   <li><b>Header {@code X-App-Token}:</b> incluido en todas las peticiones
     *       automáticamente, sin necesidad de especificarlo en cada llamada.</li>
     *   <li><b>Singleton:</b> al ser un {@code @Bean}, Spring crea una única instancia
     *       reutilizada por todos los componentes que lo inyecten.</li>
     * </ul>
     *
     * @return instancia de {@link WebClient} lista para hacer peticiones a SECOP II
     */
    @Bean
    public WebClient secopWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-App-Token", appToken)
                .build();
    }
}
