package co.edu.unbosque.sistemawebraco.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${secop.api.base-url}")
    private String baseUrl;

    @Value("${secop.api.app-token}")
    private String appToken;

    @Bean
    public WebClient secopWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-App-Token", appToken)
                .build();
    }
}


