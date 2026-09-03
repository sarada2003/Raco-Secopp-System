package co.edu.unbosque.sistemawebraco.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.reflect.Method;

/**
 * Habilita {@code @Scheduled} (usado por {@link co.edu.unbosque.sistemawebraco.services.NotificacionScheduler})
 * y {@code @Async} (usado por {@link co.edu.unbosque.sistemawebraco.services.NotificacionMailService}).
 *
 * <p>El {@link AsyncUncaughtExceptionHandler} es una red de seguridad adicional: los métodos
 * {@code @Async} de este proyecto ya capturan sus propias excepciones internamente (para que
 * un fallo de SMTP nunca tumbe la creación de la notificación en base de datos), pero si algo
 * se escapara sin capturar, aquí queda logueado en vez de perderse silenciosamente.</p>
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class SchedulingConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("Excepción no capturada en método @Async {}: {}", method.getName(), ex.getMessage(), ex);
            }
        };
    }
}
