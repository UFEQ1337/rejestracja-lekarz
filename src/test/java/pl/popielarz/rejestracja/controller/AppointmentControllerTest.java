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
import pl.popielarz.rejestracja.dto.AppointmentDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.model.AppointmentStatus;
import pl.popielarz.rejestracja.security.CurrentUser;
import pl.popielarz.rejestracja.security.SecurityErrorHandler;
import pl.popielarz.rejestracja.service.AppointmentService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, ClockConfig.class})
class AppointmentControllerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 3, 9, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    private static AppointmentDto.Response appointment(AppointmentStatus status) {
        return new AppointmentDto.Response(100L, status, "Kontrola", START.minusDays(2), null, 5L,
                START, START.plusMinutes(20), 1L, "Anna Kowalska", "101", 1L, "Mateusz Popielarz");
    }

    @Test
    @DisplayName("POST /api/v1/appointments zwraca 201 Created z Location, a pacjent jest brany z tokena")
    void shouldBookAppointment() throws Exception {
        when(appointmentService.book(any(), argThat(CurrentUser::isPatient)))
                .thenReturn(appointment(AppointmentStatus.BOOKED));

        mockMvc.perform(post("/api/v1/appointments").with(TestJwt.patient(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slotId\":5,\"reason\":\"Kontrola\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/appointments/100"))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.patientName").value("Mateusz Popielarz"));
    }

    @Test
    @DisplayName("Rezerwacja zajętego terminu zwraca 409 Conflict")
    void shouldReturnConflictForBookedSlot() throws Exception {
        when(appointmentService.book(any(), any())).thenThrow(new ConflictException("Wybrany termin jest już zajęty"));

        mockMvc.perform(post("/api/v1/appointments").with(TestJwt.patient(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slotId\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Wybrany termin jest już zajęty"));
    }

    @Test
    @DisplayName("Brak slotId w żądaniu rezerwacji daje 400")
    void shouldRequireSlotId() throws Exception {
        mockMvc.perform(post("/api/v1/appointments").with(TestJwt.patient(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Kontrola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("slotId"));
    }

    @Test
    @DisplayName("Rezerwacja bez tokena JWT zwraca 401 - API nie korzysta z sesji")
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slotId\":5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/{id} ze statusem CANCELLED anuluje wizytę")
    void shouldCancelAppointment() throws Exception {
        when(appointmentService.changeStatus(eq(100L), eq(AppointmentStatus.CANCELLED), any()))
                .thenReturn(appointment(AppointmentStatus.CANCELLED));

        mockMvc.perform(patch("/api/v1/appointments/100").with(TestJwt.patient(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
