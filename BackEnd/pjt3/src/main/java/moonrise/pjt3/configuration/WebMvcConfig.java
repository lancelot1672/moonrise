package moonrise.pjt3.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").
        allowedOrigins("i8b310.p.ssafy.io", "http://localhost:9001", "http://localhost:9002", "http://localhost:3000")
                .allowedMethods("*")
                .allowCredentials(true);
    }
}
