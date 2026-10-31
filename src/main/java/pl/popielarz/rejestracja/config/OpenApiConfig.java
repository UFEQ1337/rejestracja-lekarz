package pl.popielarz.rejestracja.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

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
}
