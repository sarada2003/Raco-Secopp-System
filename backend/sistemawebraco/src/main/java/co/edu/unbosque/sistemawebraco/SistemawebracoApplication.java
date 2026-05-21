package co.edu.unbosque.sistemawebraco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del sistema <b>RACO–SECOPP</b>.
 *
 * <p>Punto de entrada de la aplicación Spring Boot. Al ejecutarse, inicia el contexto de
 * la aplicación, configura todos los beans, levanta el servidor embebido Tomcat en el
 * puerto definido en {@code application.properties} (por defecto {@code 8090}) y deja
 * disponibles los endpoints REST.</p>
 *
 * <h3>Cómo correr la aplicación</h3>
 * <p>Desde la raíz del proyecto backend ({@code sistemawebraco/}):</p>
 * <pre>{@code
 * # Con Gradle wrapper (recomendado)
 * ./gradlew bootRun
 *
 * # O generando el JAR primero
 * ./gradlew build
 * java -jar build/libs/sistemawebraco-0.0.1-SNAPSHOT.jar
 * }</pre>
 *
 * <h3>Variables de entorno requeridas</h3>
 * <p>Antes de correr, deben estar definidas las siguientes variables:</p>
 * <ul>
 *   <li>{@code POSTGRES_USER} — usuario de la base de datos PostgreSQL</li>
 *   <li>{@code POSTGRES_PASSWORD} — contraseña de PostgreSQL</li>
 *   <li>{@code JWT_SECRET} — clave secreta para firmar tokens JWT (mínimo 64 caracteres)</li>
 *   <li>{@code SECOP_APP_TOKEN} — token de aplicación para la API de datos.gov.co</li>
 *   <li>{@code OPENAI_API_KEY} — clave de la API de OpenAI (para el módulo de chatbot IA)</li>
 * </ul>
 *
 * <h3>URLs disponibles tras el inicio</h3>
 * <ul>
 *   <li>API REST: {@code http://localhost:8090/api/}</li>
 *   <li>Swagger UI: {@code http://localhost:8090/swagger-ui/index.html}</li>
 *   <li>OpenAPI JSON: {@code http://localhost:8090/v3/api-docs}</li>
 *   <li>Actuator health: {@code http://localhost:8090/actuator/health}</li>
 * </ul>
 *
 * @author Proyecto de Grado — Universidad El Bosque
 * @version 1.0.0
 */
@SpringBootApplication
public class SistemawebracoApplication {

	/**
	 * Método principal que arranca la aplicación Spring Boot.
	 *
	 * @param args argumentos de línea de comandos (no requeridos para uso normal)
	 */
	public static void main(String[] args) {
		SpringApplication.run(SistemawebracoApplication.class, args);
	}
}
