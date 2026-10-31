package pl.popielarz.rejestracja;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test integracyjny całego przepływu na bazie H2 z migracjami Flyway:
 * logowanie, wolne terminy, rezerwacja, próba podwójnej rezerwacji, anulowanie.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    @DisplayName("Pełny scenariusz: rezerwacja, konflikt 409 przy podwójnej rezerwacji i anulowanie")
    void shouldBookCancelAndPreventDoubleBooking() throws Exception {
        String patientToken = login("mpopielarz", "Pacjent123!");
        String receptionToken = login("rejestracja", "Rejestracja123!");

        String slots = mockMvc.perform(get("/api/v1/doctors/4/slots"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode freeSlot = objectMapper.readTree(slots).get(0);
        long slotId = freeSlot.get("id").asLong();

        String created = mockMvc.perform(post("/api/v1/appointments").header("Authorization", patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slotId\":" + slotId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientName").value("Mateusz Popielarz"))
                .andReturn().getResponse().getContentAsString();
        long appointmentId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/api/v1/appointments").header("Authorization", receptionToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slotId\":" + slotId + ",\"patientId\":3}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/appointments/" + appointmentId).header("Authorization", patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        String slotAfter = mockMvc.perform(get("/api/v1/slots/" + slotId).header("Authorization", patientToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(slotAfter).get("available").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Błędne hasło daje 401, a pacjent nie widzi danych innego pacjenta (403)")
    void shouldRejectBadCredentialsAndForeignData() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mpopielarz\",\"password\":\"zle-haslo\"}"))
                .andExpect(status().isUnauthorized());

        String patientToken = login("mpopielarz", "Pacjent123!");
        mockMvc.perform(get("/api/v1/patients/2").header("Authorization", patientToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Mateusz Popielarz"));
    }
}
