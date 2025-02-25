package com.alfre.DHHotel.controller;

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

@ExtendWith(MockitoExtension.class)
public class ReservationControllerTest {

    @Mock
    private ReservationUseCase reservationUseCase;

    @InjectMocks
    private com.alfre.DHHotel.adapter.web.controller.ReservationController reservationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservationController).build();
    }

    @Test
    public void whenGetAllReservations_success_thenReturnsReservationList() throws Exception {
        // Preparar
        List<Reservation> reservationList = Arrays.asList(new Reservation(), new Reservation());
        when(reservationUseCase.getAllReservations()).thenReturn(reservationList);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservations").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(reservationUseCase, times(1)).getAllReservations();
    }

    @Test
    public void whenGetAllReservations_failure_thenReturnsNotFound() throws Exception {
        // Preparar
        when(reservationUseCase.getAllReservations()).thenReturn(Collections.emptyList());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservations").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay reservas registradas en el sistema."))
                .andDo(print());

        verify(reservationUseCase).getAllReservations();
    }

    @Test
    public void whenGetReservationById_success_thenReturnsReservation() throws Exception {
        // Preparar
        int reservationId = 1;
        Reservation reservation = new Reservation();

        when(reservationUseCase.getReservationById(reservationId)).thenReturn(Optional.of(reservation));

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservation/{id}", reservationId).with(csrf()))
                .andExpect(status().isOk())
                .andDo(print());

        verify(reservationUseCase).getReservationById(reservationId);
    }

    @Test
    public void whenGetReservationById_notFound_thenReturns404() throws Exception {
        // Preparar
        int nonExistentId = 999;
        when(reservationUseCase.getReservationById(nonExistentId)).thenReturn(Optional.empty());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservation/{id}", nonExistentId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("La reserva solicitada no existe"))
                .andDo(print());
    }

    @Test
    public void whenGetReservationsByClient_success_thenReturnsReservationList() throws Exception {
        // Preparar
        List<Reservation> reservations = Arrays.asList(new Reservation(), new Reservation());
        when(reservationUseCase.getReservationsByClient(any(User.class))).thenReturn(reservations);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/client/reservations/my")
                        .with(csrf())
                        .principal(() -> "dummyUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(reservationUseCase).getReservationsByClient(any(User.class));
    }

    @Test
    public void whenGetReservationsByClient_failure_thenReturnsNotFound() throws Exception {
        // Preparar
        when(reservationUseCase.getReservationsByClient(any(User.class))).thenReturn(Collections.emptyList());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/client/reservations/my")
                        .with(csrf())
                        .principal(() -> "dummyUser"))
                .andExpect(status().isNotFound())
                .andExpect(content()
                        .string("No hay reservas asociadas al cliente registradas en el sistema."))
                .andDo(print());

        verify(reservationUseCase).getReservationsByClient(any(User.class));
    }

    @Test
    public void whenCreateReservation_success_thenReturnsReservation() throws Exception {
        // Se simula que el caso de uso devuelve la misma reserva (o una nueva con el identificador asignado)
        when(reservationUseCase.createReservation(any(Reservation.class), any(User.class)))
                .thenReturn(1L);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/client/reservation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isOk())
                .andDo(print());

        verify(reservationUseCase).createReservation(any(Reservation.class), any(User.class));
    }

    @Test
    public void whenCreateReservation_failure_thenReturnsBadRequest() throws Exception {
        // Preparar
        when(reservationUseCase.createReservation(any(Reservation.class), any(User.class)))
                .thenThrow(new RuntimeException("Error creando reserva"));

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/client/reservation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error creando reserva"))
                .andDo(print());
    }

    @Test
    public void whenUpdateReservation_success_thenReturnsOk() throws Exception {
        // Preparar
        long reservationId = 1L;

        when(reservationUseCase.updateReservation(eq(reservationId), any(Reservation.class), any(User.class)))
                .thenReturn(1);

        // Ejecutar y Verificar
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

    @Test
    public void whenUpdateReservation_failure_thenReturnsInternalServerError() throws Exception {
        // Preparar
        long reservationId = 1L;

        when(reservationUseCase.updateReservation(eq(reservationId), any(Reservation.class), any(User.class)))
                .thenReturn(0);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/reservation/{id}", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());
    }

    @Test
    public void whenUpdateReservation_accessDenied_thenReturnsForbidden() throws Exception {
        // Preparar: Simular que el use case lanza una AccessDeniedException con el mensaje "No autorizado"
        long reservationId = 1L;

        when(reservationUseCase.updateReservation(eq(reservationId), any(Reservation.class), any(User.class)))
                .thenThrow(new AccessDeniedException("No autorizado"));

        // Ejecutar y Verificar
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

    @Test
    public void whenUpdateReservation_exception_thenReturnsBadRequest() throws Exception {
        // Preparar
        long reservationId = 1L;

        when(reservationUseCase.updateReservation(eq(reservationId), any(Reservation.class), any(User.class)))
                .thenThrow(new RuntimeException("Error actualizando reserva"));

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/reservation/{id}", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error actualizando reserva"))
                .andDo(print());
    }

    @Test
    public void whenCancelReservation_success_thenReturnsOk() throws Exception {
        // Preparar
        long reservationId = 1L;
        when(reservationUseCase.cancelReservation(reservationId)).thenReturn(1);

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/reservation/{id}", reservationId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("La cancelación se ha hecho correctamente"))
                .andDo(print());

        verify(reservationUseCase).cancelReservation(reservationId);
    }

    @Test
    public void whenCancelReservation_failure_thenReturnsInternalServerError() throws Exception {
        // Preparar
        long reservationId = 999L;
        when(reservationUseCase.cancelReservation(reservationId)).thenReturn(0);

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/reservation/{id}", reservationId).with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());

        verify(reservationUseCase).cancelReservation(reservationId);
    }

    @Test
    public void whenCancelReservation_exception_thenReturnsBadRequest() throws Exception {
        // Preparar
        long reservationId = 1L;
        when(reservationUseCase.cancelReservation(reservationId))
                .thenThrow(new RuntimeException("Error cancelando reserva"));

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/reservation/{id}", reservationId).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error cancelando reserva"))
                .andDo(print());
    }

    @Test
    public void whenCreatePaymentOfReservation_success_thenReturnsPayment() throws Exception {
        // Preparar
        long reservationId = 1L;
        when(reservationUseCase.createPayment(any(Payment.class), eq(reservationId), any(User.class)))
                .thenReturn(2L);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/admin/reservation/{id}/payment", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isOk())
                .andDo(print());

        verify(reservationUseCase).createPayment(any(Payment.class), eq(reservationId), any(User.class));
    }

    @Test
    public void whenCreatePaymentOfReservation_failure_thenReturnsBadRequest() throws Exception {
        // Preparar
        long reservationId = 1L;
        when(reservationUseCase.createPayment(any(Payment.class), eq(reservationId), any(User.class)))
                .thenThrow(new RuntimeException("Error creando pago"));

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/admin/reservation/{id}/payment", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .principal(() -> "dummyUser"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error creando pago"))
                .andDo(print());
    }

    @Test
    public void whenGetPaymentsByClient_success_thenReturnsPaymentList() throws Exception {
        // Preparar
        long reservationId = 1L;
        List<Payment> payments = Arrays.asList(new Payment(), new Payment());
        when(reservationUseCase.getPaymentsByClient(reservationId)).thenReturn(payments);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservations/{id}/payments", reservationId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(reservationUseCase).getPaymentsByClient(reservationId);
    }

    @Test
    public void whenGetPaymentsByClient_failure_thenReturnsNotFound() throws Exception {
        // Preparar
        long reservationId = 1L;
        when(reservationUseCase.getPaymentsByClient(reservationId)).thenReturn(Collections.emptyList());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservations/{id}/payments", reservationId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content()
                        .string("No hay pagos asociados al cliente registrados en el sistema."))
                .andDo(print());

        verify(reservationUseCase).getPaymentsByClient(reservationId);
    }
}