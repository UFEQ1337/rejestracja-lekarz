package pl.popielarz.rejestracja.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.popielarz.rejestracja.dto.ErrorResponse;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Przychodnia Popielarz - API rejestracji do lekarza",
        version = "v1",
        description = """
                REST API systemu rejestracji pacjentów do lekarzy. Zasoby publiczne: lista \
                specjalizacji, lekarzy i wolnych terminów. Pozostałe operacje wymagają tokena JWT \
                uzyskanego z POST /api/v1/auth/login (przycisk Authorize).""",
        contact = @Contact(name = "Mateusz Popielarz")))
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    /**
     * Wszystkie odpowiedzi 4xx i 5xx w dokumentacji mają schemat ErrorResponse,
     * tak jak zwraca je GlobalExceptionHandler.
     */
    @Bean
    OpenApiCustomizer errorResponseCustomizer() {
        return openApi -> {
            ModelConverters.getInstance().read(ErrorResponse.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            Content errorContent = new Content().addMediaType("application/json",
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                    operation.getResponses().forEach((code, response) -> {
                        if (code.startsWith("4") || code.startsWith("5")) {
                            response.setContent(errorContent);
                        }
                    })));
        };
    }
}
