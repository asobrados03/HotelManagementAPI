package com.alfre.DHHotel.controller;

import com.alfre.DHHotel.adapter.web.controller.PaymentController;
import com.alfre.DHHotel.domain.model.Payment;
import com.alfre.DHHotel.usecase.PaymentUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PaymentControllerTest {
    @Mock
    private PaymentUseCase paymentUseCase;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    @Test
    public void whenGetAllPayments_success_thenReturnsPaymentList() throws Exception {
        // Preparar
        List<Payment> paymentList = Arrays.asList(new Payment(), new Payment());
        when(paymentUseCase.getAllPayments()).thenReturn(paymentList);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payments").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(paymentUseCase, times(1)).getAllPayments();
    }

    @Test
    public void whenGetAllPayments_failure_thenReturnsNotFound() throws Exception {
        // Preparar
        List<Payment> paymentList = new ArrayList<>();

        when(paymentUseCase.getAllPayments()).thenReturn(paymentList);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payments")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos registrados en el sistema."));

        verify(paymentUseCase).getAllPayments();
    }

    @Test
    public void whenGetPaymentById_success_thenReturnsPayment() throws Exception {
        // Preparar
        long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        when(paymentUseCase.getPaymentById(paymentId)).thenReturn(Optional.of(payment));

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payment/{id}", paymentId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andDo(print());

        verify(paymentUseCase).getPaymentById(paymentId);
    }

    @Test
    public void whenGetPaymentById_notFound_thenReturns404() throws Exception {
        // Preparar
        long nonExistentId = 999L;
        when(paymentUseCase.getPaymentById(nonExistentId)).thenReturn(Optional.empty());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payment/{id}", nonExistentId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El pago solicitado no existe"))
                .andDo(print());
    }

    // Tests para getPaymentsByReservationId
    @Test
    public void whenGetPaymentsByReservationId_success_thenReturnsPaymentList() throws Exception {
        // Preparar
        long reservationId = 1L;
        List<Payment> payments = Arrays.asList(new Payment(), new Payment());
        when(paymentUseCase.getPaymentsByReservationId(reservationId)).thenReturn(payments);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payment/reservation/id/{reservationId}", reservationId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(paymentUseCase).getPaymentsByReservationId(reservationId);
    }

    @Test
    public void whenGetPaymentsByReservationId_failure_thenReturnsNotFound() throws Exception {
        // Preparar
        long reservationId = 1L;
        List<Payment> payments = new ArrayList<>();
        when(paymentUseCase.getPaymentsByReservationId(reservationId)).thenReturn(payments);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payment/reservation/id/{reservationId}", reservationId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content()
                        .string("No hay pagos registrados asociados a la reserva en el sistema."));

        verify(paymentUseCase).getPaymentsByReservationId(reservationId);
    }

    @Test
    public void whenUpdatePayment_success_thenReturnsOk() throws Exception {
        // Preparar
        long paymentId = 1L;
        Payment payment = new Payment();
        when(paymentUseCase.updatePayment(any(Payment.class), eq(paymentId))).thenReturn(1);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/superadmin/payment/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"))
                .andDo(print());
    }

    @Test
    public void whenUpdatePayment_failure_thenReturnsBadRequest() throws Exception {
        // Preparar
        long paymentId = 1L;
        when(paymentUseCase.updatePayment(any(), eq(paymentId))).thenReturn(0);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/superadmin/payment/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());
    }

    @Test
    public void whenUpdatePayment_exception_thenReturnsInternalError() throws Exception {
        // Preparar
        long paymentId = 1L;
        when(paymentUseCase.updatePayment(any(), eq(paymentId))).thenThrow(new RuntimeException("Error de base de datos"));

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/superadmin/payment/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de base de datos"))
                .andDo(print());
    }

    @Test
    public void whenDeletePayment_success_thenReturnsOk() throws Exception {
        // Preparar
        long paymentId = 1L;
        when(paymentUseCase.deletePayment(paymentId)).thenReturn(1);

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/superadmin/payment/{id}", paymentId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("El pago con id: 1 se ha eliminado correctamente"))
                .andDo(print());
    }

    @Test
    public void whenDeletePayment_failure_thenReturnsBadRequest() throws Exception {
        // Preparar
        long paymentId = 999L;
        when(paymentUseCase.deletePayment(paymentId)).thenReturn(0);

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/superadmin/payment/{id}", paymentId).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido eliminar."))
                .andDo(print());
    }

    @Test
    public void whenDeletePayment_exception_thenReturnsInternalError() throws Exception {
        // Preparar
        long paymentId = 1L;
        when(paymentUseCase.deletePayment(paymentId)).thenThrow(new RuntimeException());

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/superadmin/payment/{id}", paymentId).with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."))
                .andDo(print());
    }
}