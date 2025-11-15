package cl.ufro.dci.naivepayapi.recompensas.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**configuracion de cors para que el frontend acceda a los endpoints de recompensa*/

@Configuration
public class RewardsConfig {
    @Bean
    public WebMvcConfigurer rewardsCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/rewards/**")
                        .allowedOrigins("http://localhost:4200")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}