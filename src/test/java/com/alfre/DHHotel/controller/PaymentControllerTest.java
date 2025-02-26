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

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the payments operations controller.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class PaymentControllerTest {
    @Mock
    private PaymentUseCase paymentUseCase;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;

    /**
     * Configures MockMvc in standalone mode with the PaymentController before each test.
     */
    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    /**
     * Tests that when retrieving all payments successfully, the endpoint returns a JSON list containing the expected number of payments.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetAllPayments_success_thenReturnsPaymentList() throws Exception {
        // Arrange: Prepare a list of payments.
        List<Payment> paymentList = Arrays.asList(new Payment(), new Payment());
        when(paymentUseCase.getAllPayments()).thenReturn(paymentList);

        // Act & Assert: Perform GET request and verify that the response contains two payments.
        mockMvc.perform(get("/api/admin/payments").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(paymentUseCase, times(1)).getAllPayments();
    }

    /**
     * Tests that when no payments are found, the endpoint returns a 404 Not Found with an appropriate message.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetAllPayments_failure_thenReturnsNotFound() throws Exception {
        // Arrange: Prepare an empty payment list.
        List<Payment> paymentList = new ArrayList<>();
        when(paymentUseCase.getAllPayments()).thenReturn(paymentList);

        // Act & Assert: Perform GET request and verify that a 404 status is returned with the expected message.
        mockMvc.perform(get("/api/admin/payments")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos registrados en el sistema."));

        verify(paymentUseCase).getAllPayments();
    }

    /**
     * Tests that when retrieving a payment by a valid ID, the endpoint returns the expected payment.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetPaymentById_success_thenReturnsPayment() throws Exception {
        // Arrange: Prepare a sample payment with a specific ID.
        long paymentId = 1L;
        Payment payment = new Payment();
        payment.setId(paymentId);
        when(paymentUseCase.getPaymentById(paymentId)).thenReturn(Optional.of(payment));

        // Act & Assert: Perform GET request and verify the returned JSON contains the correct payment ID.
        mockMvc.perform(get("/api/admin/payment/{id}", paymentId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andDo(print());

        verify(paymentUseCase).getPaymentById(paymentId);
    }

    /**
     * Tests that when retrieving a payment by an ID that does not exist,
     * the endpoint returns a 404 Not Found with the appropriate error message.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetPaymentById_notFound_thenReturns404() throws Exception {
        // Arrange: Use a non-existent payment ID.
        long nonExistentId = 999L;
        when(paymentUseCase.getPaymentById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert: Perform GET request and verify a 404 status with an error message.
        mockMvc.perform(get("/api/admin/payment/{id}", nonExistentId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El pago solicitado no existe"))
                .andDo(print());
    }

    /**
     * Tests that when retrieving payments by reservation ID successfully, the endpoint returns a list of payments.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetPaymentsByReservationId_success_thenReturnsPaymentList() throws Exception {
        // Arrange: Prepare a list of payments associated with a reservation.
        long reservationId = 1L;
        List<Payment> payments = Arrays.asList(new Payment(), new Payment());
        when(paymentUseCase.getPaymentsByReservationId(reservationId)).thenReturn(payments);

        // Act & Assert: Perform GET request and verify that the response contains the expected number of payments.
        mockMvc.perform(get("/api/admin/payment/reservation/id/{reservationId}", reservationId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(paymentUseCase).getPaymentsByReservationId(reservationId);
    }

    /**
     * Tests that when no payments are associated with a reservation, the endpoint returns a 404 Not Found.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetPaymentsByReservationId_failure_thenReturnsNotFound() throws Exception {
        // Arrange: Prepare an empty list for a reservation.
        long reservationId = 1L;
        List<Payment> payments = new ArrayList<>();
        when(paymentUseCase.getPaymentsByReservationId(reservationId)).thenReturn(payments);

        // Act & Assert: Perform GET request and verify that a 404 status is returned with the expected message.
        mockMvc.perform(get("/api/admin/payment/reservation/id/{reservationId}", reservationId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos registrados asociados a la reserva en el sistema."));

        verify(paymentUseCase).getPaymentsByReservationId(reservationId);
    }

    /**
     * Tests that updating a payment with valid data returns a success message.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenUpdatePayment_success_thenReturnsOk() throws Exception {
        // Arrange: Prepare a payment update scenario.
        long paymentId = 1L;
        Payment payment = new Payment();
        when(paymentUseCase.updatePayment(any(Payment.class), eq(paymentId))).thenReturn(1);

        // Act & Assert: Perform PUT request and verify the success message.
        mockMvc.perform(put("/api/superadmin/payment/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"))
                .andDo(print());
    }

    /**
     * Tests that when updating a payment fails (e.g., no rows updated), the endpoint returns a 400 Bad Request.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenUpdatePayment_failure_thenReturnsBadRequest() throws Exception {
        // Arrange: Stub the updatePayment method to return 0 (failure).
        long paymentId = 1L;
        when(paymentUseCase.updatePayment(any(), eq(paymentId))).thenReturn(0);

        // Act & Assert: Perform PUT request and verify that a 400 Bad Request is returned with the expected message.
        mockMvc.perform(put("/api/superadmin/payment/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());
    }

    /**
     * Tests that when updating a payment throws an exception, the endpoint returns a 500 Internal Server Error.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenUpdatePayment_exception_thenReturnsInternalError() throws Exception {
        // Arrange: Stub updatePayment to throw a RuntimeException.
        long paymentId = 1L;
        when(paymentUseCase.updatePayment(any(), eq(paymentId))).thenThrow(new RuntimeException("Error de base de datos"));

        // Act & Assert: Perform PUT request and verify that a 500 Internal Server Error is returned with the exception message.
        mockMvc.perform(put("/api/superadmin/payment/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de base de datos"))
                .andDo(print());
    }

    /**
     * Tests that when deleting a payment successfully, the endpoint returns a success message.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenDeletePayment_success_thenReturnsOk() throws Exception {
        // Arrange: Prepare a scenario where deletePayment returns 1.
        long paymentId = 1L;
        when(paymentUseCase.deletePayment(paymentId)).thenReturn(1);

        // Act & Assert: Perform DELETE request and verify the success message.
        mockMvc.perform(delete("/api/superadmin/payment/{id}", paymentId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("El pago con id: 1 se ha eliminado correctamente"))
                .andDo(print());
    }

    /**
     * Tests that when deleting a payment fails (returns 0), the endpoint returns a 400 Bad Request.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenDeletePayment_failure_thenReturnsBadRequest() throws Exception {
        // Arrange: Prepare a scenario where deletion fails.
        long paymentId = 999L;
        when(paymentUseCase.deletePayment(paymentId)).thenReturn(0);

        // Act & Assert: Perform DELETE request and verify that a 400 status is returned with the expected message.
        mockMvc.perform(delete("/api/superadmin/payment/{id}", paymentId).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido eliminar."))
                .andDo(print());
    }

    /**
     * Tests that when deleting a payment throws an exception, the endpoint returns a 500 Internal Server Error.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenDeletePayment_exception_thenReturnsInternalError() throws Exception {
        // Arrange: Prepare a scenario where deletion throws an exception.
        long paymentId = 1L;
        when(paymentUseCase.deletePayment(paymentId)).thenThrow(new RuntimeException());

        // Act & Assert: Perform DELETE request and verify that a 500 status is returned with the error message.
        mockMvc.perform(delete("/api/superadmin/payment/{id}", paymentId).with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."))
                .andDo(print());
    }
}