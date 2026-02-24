package com.icthh.xm.uaa.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.icthh.xm.uaa.config.Constants.SPRING_PROFILE_API_DOCS;

@Configuration
@Profile(SPRING_PROFILE_API_DOCS)
@OpenAPIDefinition(info = @Info(title = "UAA API", description = "UAA API Documentation"))
public class OpenApiConfiguration {

}
