package pl.popielarz.rejestracja.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.popielarz.rejestracja.config.ClockConfig;
import pl.popielarz.rejestracja.config.SecurityConfig;
import pl.popielarz.rejestracja.dto.DoctorDto;
import pl.popielarz.rejestracja.dto.SpecializationDto;
import pl.popielarz.rejestracja.exception.ResourceNotFoundException;
import pl.popielarz.rejestracja.security.SecurityErrorHandler;
import pl.popielarz.rejestracja.service.DoctorService;
import pl.popielarz.rejestracja.service.TimeSlotService;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, ClockConfig.class})
class DoctorControllerTest {

    private static final DoctorDto.Response KOWALSKA = new DoctorDto.Response(1L, "Anna", "Kowalska",
            "Anna Kowalska", "1234561", "anna.kowalska@przychodnia-popielarz.pl", null, "101",
            List.of(new SpecializationDto.Response(3L, "Kardiologia")));

    private static final String VALID_DOCTOR = """
            {"firstName":"Adam","lastName":"Mazur","licenseNumber":"7777777",
             "email":"adam.mazur@przychodnia-popielarz.pl","specializationIds":[6]}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorService doctorService;
    @MockitoBean
    private TimeSlotService timeSlotService;

    @Test
    @DisplayName("GET /api/v1/doctors jest publiczny i zwraca 200 z listą lekarzy")
    void shouldReturnDoctorListWithoutToken() throws Exception {
        when(doctorService.findAll(null)).thenReturn(List.of(KOWALSKA));

        mockMvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Anna Kowalska"))
                .andExpect(jsonPath("$[0].specializations[0].name").value("Kardiologia"));
    }

    @Test
    @DisplayName("GET /api/v1/doctors/{id} dla nieistniejącego lekarza zwraca 404 w formacie ErrorResponse")
    void shouldReturnNotFoundInUnifiedFormat() throws Exception {
        when(doctorService.findById(99L)).thenThrow(ResourceNotFoundException.of("Lekarz", 99L));

        mockMvc.perform(get("/api/v1/doctors/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Lekarz o id 99 nie istnieje"))
                .andExpect(jsonPath("$.path").value("/api/v1/doctors/99"));
    }

    @Test
    @DisplayName("POST /api/v1/doctors z błędnymi danymi zwraca 400 z listą pól")
    void shouldReturnValidationErrors() throws Exception {
        String invalid = """
                {"firstName":"","lastName":"Mazur","licenseNumber":"12","email":"zly-adres","specializationIds":[]}""";

        mockMvc.perform(post("/api/v1/doctors").with(TestJwt.receptionist())
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[*].field", hasItem("licenseNumber")))
                .andExpect(jsonPath("$.violations[*].field", hasItem("email")))
                .andExpect(jsonPath("$.violations[*].field", hasItem("specializationIds")));
        verify(doctorService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/v1/doctors przez rejestratorkę zwraca 201 i nagłówek Location")
    void shouldCreateDoctor() throws Exception {
        when(doctorService.create(any())).thenReturn(KOWALSKA);

        mockMvc.perform(post("/api/v1/doctors").with(TestJwt.receptionist())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_DOCTOR))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/doctors/1"));
    }

    @Test
    @DisplayName("Pacjent nie może dodać lekarza (403), a bez tokena żądanie jest odrzucane (401)")
    void shouldProtectDoctorCreation() throws Exception {
        mockMvc.perform(post("/api/v1/doctors").with(TestJwt.patient(1L))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_DOCTOR))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_DOCTOR))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    @DisplayName("DELETE /api/v1/doctors/{id} zwraca 204 No Content")
    void shouldDeactivateDoctor() throws Exception {
        mockMvc.perform(delete("/api/v1/doctors/1").with(TestJwt.receptionist()))
                .andExpect(status().isNoContent());
        verify(doctorService).deactivate(1L);
    }
}
