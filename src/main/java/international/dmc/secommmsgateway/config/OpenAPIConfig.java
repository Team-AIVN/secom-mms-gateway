package international.dmc.secommmsgateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI secomMmsAPI() {
        return new OpenAPI()
                .info(new Info().title("DMC SECOM MMS Gateway Service")
                        .description("Service for acting as a gateway from SECOM to MMS")
                        .version("1.0.0")
                );
    }
}
