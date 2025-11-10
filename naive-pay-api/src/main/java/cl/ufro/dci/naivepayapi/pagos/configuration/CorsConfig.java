package cl.ufro.dci.naivepayapi.pagos.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for CORS (Cross-Origin Resource Sharing) settings.
 * Enables cross-origin requests for the payments API from the frontend application.
 */
@Configuration
public class CorsConfig {
    
    /**
     * Configures CORS mappings for the payments API endpoints.
     * Allows requests from the frontend application running on localhost:4200.
     *
     * @return WebMvcConfigurer with CORS configuration for API endpoints
     */
    @Bean
    public WebMvcConfigurer paymentsCorsConfigurer() {
        return new WebMvcConfigurer() {
            /**
             * Configures CORS settings for specific path patterns.
             *
             * @param registry the CorsRegistry to configure CORS mappings
             */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:4200")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}