package com.alfre.DHHotel.controller;

import com.alfre.DHHotel.adapter.web.controller.ReservationController;
import com.alfre.DHHotel.domain.model.Payment;
import com.alfre.DHHotel.domain.model.Reservation;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.usecase.ReservationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the reservations operations controller.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class ReservationControllerTest {

    @Mock
    private ReservationUseCase reservationUseCase;

    @InjectMocks
    private ReservationController reservationController;

    private MockMvc mockMvc;

    /**
     * Sets up the testing environment before each test by configuring MockMvc
     * with a standalone setup for the reservationController.
     */
    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservationController).build();
    }

    /**
     * Tests that when retrieving all reservations, the API returns a list of reservations successfully.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenGetAllReservations_success_thenReturnsReservationList() throws Exception {
        // Prepare test data
        List<Reservation> reservationList = Arrays.asList(new Reservation(), new Reservation());
        when(reservationUseCase.getAllReservations()).thenReturn(reservationList);

        // Execute and Verify
        mockMvc.perform(get("/api/admin/reservations").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(reservationUseCase, times(1)).getAllReservations();
    }

    /**
     * Tests that when no reservations exist, the API returns a 404 Not Found status with an appropriate message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenGetAllReservations_failure_thenReturnsNotFound() throws Exception {
        // Prepare test data
        when(reservationUseCase.getAllReservations()).thenReturn(Collections.emptyList());

        // Execute and Verify
        mockMvc.perform(get("/api/admin/reservations").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay reservas registradas en el sistema."))
                .andDo(print());

        verify(reservationUseCase).getAllReservations();
    }

    /**
     * Tests that retrieving a reservation by a valid ID returns the reservation details successfully.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenGetReservationById_success_thenReturnsReservation() throws Exception {
        // Prepare test data
        int reservationId = 1;
        Reservation reservation = new Reservation();
        when(reservationUseCase.getReservationById(reservationId)).thenReturn(Optional.of(reservation));

        // Execute and Verify
        mockMvc.perform(get("/api/admin/reservation/{id}", reservationId).with(csrf()))
                .andExpect(status().isOk())
                .andDo(print());

        verify(reservationUseCase).getReservationById(reservationId);
    }

    /**
     * Tests that retrieving a reservation by an invalid ID returns a 404 Not Found status with an error message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenGetReservationById_notFound_thenReturns404() throws Exception {
        // Prepare test data
        int nonExistentId = 999;
        when(reservationUseCase.getReservationById(nonExistentId)).thenReturn(Optional.empty());

        // Execute and Verify
        mockMvc.perform(get("/api/admin/reservation/{id}", nonExistentId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("La reserva solicitada no existe"))
                .andDo(print());
    }

    /**
     * Tests that when a client requests their reservations and reservations exist, the API returns the correct list.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenGetReservationsByClient_success_thenReturnsReservationList() throws Exception {
        // Prepare test data
        List<Reservation> reservations = Arrays.asList(new Reservation(), new Reservation());
        when(reservationUseCase.getReservationsByClient(any(User.class))).thenReturn(reservations);

        // Execute and Verify
        mockMvc.perform(get("/api/client/reservations/my")
                        .with(csrf())
                        .principal(() -> "dummyUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(reservationUseCase).getReservationsByClient(any(User.class));
    }

    /**
     * Tests that when a client has no associated reservations, the API returns a 404 Not Found status with an
     * appropriate message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenGetReservationsByClient_failure_thenReturnsNotFound() throws Exception {
        // Prepare test data
        when(reservationUseCase.getReservationsByClient(any(User.class))).thenReturn(Collections.emptyList());

        // Execute and Verify
        mockMvc.perform(get("/api/client/reservations/my")
                        .with(csrf())
                        .principal(() -> "dummyUser"))
                .andExpect(status().isNotFound())
                .andExpect(content()
                        .string("No hay reservas asociadas al cliente registradas en el sistema."))
                .andDo(print());

        verify(reservationUseCase).getReservationsByClient(any(User.class));
    }

    /**
     * Tests that creating a reservation returns a successful response with the reservation's identifier.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenCreateReservation_success_thenReturnsReservation() throws Exception {
        // Simulate that the use case returns a new reservation id
        when(reservationUseCase.createReservation(any(Reservation.class), any(User.class)))
                .thenReturn(1L);

        // Execute and Verify
        mockMvc.perform(post("/api/client/reservation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isOk())
                .andDo(print());

        verify(reservationUseCase).createReservation(any(Reservation.class), any(User.class));
    }

    /**
     * Tests that when an error occurs during the creation of a reservation, the API returns a 400 Bad Request
     * status with an error message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenCreateReservation_failure_thenReturnsBadRequest() throws Exception {
        // Prepare test data
        when(reservationUseCase.createReservation(any(Reservation.class), any(User.class)))
                .thenThrow(new RuntimeException("Error creando reserva"));

        // Execute and Verify
        mockMvc.perform(post("/api/client/reservation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error creando reserva"))
                .andDo(print());
    }

    /**
     * Tests that updating a reservation successfully returns a 200 OK status with a success message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenUpdateReservation_success_thenReturnsOk() throws Exception {
        // Prepare test data
        long reservationId = 1L;
        when(reservationUseCase.updateReservation(eq(reservationId), any(Reservation.class), any(User.class)))
                .thenReturn(1);

        // Execute and Verify
        mockMvc.perform(put("/api/reservation/{id}", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"))
                .andDo(print());

        verify(reservationUseCase).updateReservation(eq(reservationId), any(Reservation.class), any(User.class));
    }

    /**
     * Tests that if updating a reservation fails (i.e., no records are updated), the API returns a 500 Internal
     * Server Error status with a failure message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenUpdateReservation_failure_thenReturnsInternalServerError() throws Exception {
        // Prepare test data
        long reservationId = 1L;
        when(reservationUseCase.updateReservation(eq(reservationId), any(Reservation.class), any(User.class)))
                .thenReturn(0);

        // Execute and Verify
        mockMvc.perform(put("/api/reservation/{id}", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());
    }

    /**
     * Tests that when updating a reservation results in an AccessDeniedException,
     * the API returns a 403 Forbidden status with the appropriate error message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenUpdateReservation_accessDenied_thenReturnsForbidden() throws Exception {
        // Prepare test data: simulate AccessDeniedException with message "No autorizado"
        long reservationId = 1L;
        when(reservationUseCase.updateReservation(eq(reservationId), any(Reservation.class), any(User.class)))
                .thenThrow(new AccessDeniedException("No autorizado"));

        // Execute and Verify
        mockMvc.perform(put("/api/reservation/{id}", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("No autorizado"))
                .andDo(print());

        verify(reservationUseCase).updateReservation(eq(reservationId), any(Reservation.class), any(User.class));
    }

    /**
     * Tests that when an exception is thrown during reservation update,
     * the API returns a 400 Bad Request status with the error message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenUpdateReservation_exception_thenReturnsBadRequest() throws Exception {
        // Prepare test data
        long reservationId = 1L;
        when(reservationUseCase.updateReservation(eq(reservationId), any(Reservation.class), any(User.class)))
                .thenThrow(new RuntimeException("Error actualizando reserva"));

        // Execute and Verify
        mockMvc.perform(put("/api/reservation/{id}", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error actualizando reserva"))
                .andDo(print());
    }

    /**
     * Tests that cancelling a reservation successfully returns a 200 OK status with a success message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenCancelReservation_success_thenReturnsOk() throws Exception {
        // Prepare test data
        long reservationId = 1L;
        when(reservationUseCase.cancelReservation(reservationId)).thenReturn(1);

        // Execute and Verify
        mockMvc.perform(delete("/api/admin/reservation/{id}", reservationId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("La cancelación se ha hecho correctamente"))
                .andDo(print());

        verify(reservationUseCase).cancelReservation(reservationId);
    }

    /**
     * Tests that if cancellation of a reservation fails,
     * the API returns a 500 Internal Server Error status with an error message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenCancelReservation_failure_thenReturnsInternalServerError() throws Exception {
        // Prepare test data
        long reservationId = 999L;
        when(reservationUseCase.cancelReservation(reservationId)).thenReturn(0);

        // Execute and Verify
        mockMvc.perform(delete("/api/admin/reservation/{id}", reservationId).with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());

        verify(reservationUseCase).cancelReservation(reservationId);
    }

    /**
     * Tests that when an exception occurs during reservation cancellation,
     * the API returns a 400 Bad Request status with the error message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenCancelReservation_exception_thenReturnsBadRequest() throws Exception {
        // Prepare test data
        long reservationId = 1L;
        when(reservationUseCase.cancelReservation(reservationId))
                .thenThrow(new RuntimeException("Error cancelando reserva"));

        // Execute and Verify
        mockMvc.perform(delete("/api/admin/reservation/{id}", reservationId).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error cancelando reserva"))
                .andDo(print());
    }

    /**
     * Tests that creating a payment for a reservation returns a successful response with the payment's identifier.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenCreatePaymentOfReservation_success_thenReturnsPayment() throws Exception {
        // Prepare test data
        long reservationId = 1L;
        when(reservationUseCase.createPayment(any(Payment.class), eq(reservationId), any(User.class)))
                .thenReturn(2L);

        // Execute and Verify
        mockMvc.perform(post("/api/admin/reservation/{id}/payment", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isOk())
                .andDo(print());

        verify(reservationUseCase).createPayment(any(Payment.class), eq(reservationId), any(User.class));
    }

    /**
     * Tests that when an error occurs during the creation of a payment for a reservation,
     * the API returns a 400 Bad Request status with an error message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenCreatePaymentOfReservation_failure_thenReturnsBadRequest() throws Exception {
        // Prepare test data
        long reservationId = 1L;
        when(reservationUseCase.createPayment(any(Payment.class), eq(reservationId), any(User.class)))
                .thenThrow(new RuntimeException("Error creando pago"));

        // Execute and Verify
        mockMvc.perform(post("/api/admin/reservation/{id}/payment", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error creando pago"))
                .andDo(print());
    }

    /**
     * Tests that retrieving payments for a given reservation returns a successful response
     * with the correct list of payments.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenGetPaymentsByClient_success_thenReturnsPaymentList() throws Exception {
        // Prepare test data
        long reservationId = 1L;
        List<Payment> payments = Arrays.asList(new Payment(), new Payment());
        when(reservationUseCase.getPaymentsByClient(reservationId)).thenReturn(payments);

        // Execute and Verify
        mockMvc.perform(get("/api/admin/reservations/{id}/payments", reservationId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(reservationUseCase).getPaymentsByClient(reservationId);
    }

    /**
     * Tests that when no payments are found for a given reservation,
     * the API returns a 404 Not Found status with an appropriate error message.
     *
     * @throws Exception if an error occurs during test execution.
     */
    @Test
    public void whenGetPaymentsByClient_failure_thenReturnsNotFound() throws Exception {
        // Prepare test data
        long reservationId = 1L;
        when(reservationUseCase.getPaymentsByClient(reservationId)).thenReturn(Collections.emptyList());

        // Execute and Verify
        mockMvc.perform(get("/api/admin/reservations/{id}/payments", reservationId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos asociados al cliente registrados en el sistema."))
                .andDo(print());

        verify(reservationUseCase).getPaymentsByClient(reservationId);
    }
}