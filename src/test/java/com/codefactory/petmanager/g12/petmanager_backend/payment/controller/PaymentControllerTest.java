package com.codefactory.petmanager.g12.petmanager_backend.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.codefactory.petmanager.g12.petmanager_backend.common.exceptions.GlobalExceptionHandler;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.PaymentRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.PaymentResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.SupplierLastNextPaymentsResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.SupplierPaymentsResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.UpcomingPaymentAlertDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.model.PaymentCondition;
import com.codefactory.petmanager.g12.petmanager_backend.payment.service.PaymentService;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock private PaymentService paymentService;

    @InjectMocks private PaymentController paymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    private static String paymentJson(LocalDate date, String quantity, String price) {
        return """
            {
              "supplierId": 1,
              "paymentDate": "%s",
              "products": [
                { "product": { "name": "Dog Chow", "brand": "Purina" }, "quantity": %s, "pricePerUnit": %s }
              ],
              "notes": "Pago mensual"
            }
            """.formatted(date, quantity, price);
    }

    // ---------- crear ----------

    @Test
    void createPayment_whenValid_returns201() throws Exception {
        LocalDate date = LocalDate.now().plusDays(5);
        PaymentResponseDTO response = new PaymentResponseDTO(7, date, new BigDecimal("21.00"), List.of(), "Pago mensual");
        when(paymentService.createPayment(any(PaymentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson(date, "2", "10.50")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(7))
            .andExpect(jsonPath("$.amount").value(21.00));

        ArgumentCaptor<PaymentRequestDTO> captor = ArgumentCaptor.forClass(PaymentRequestDTO.class);
        verify(paymentService).createPayment(captor.capture());
        assertEquals(1, captor.getValue().getSupplierId());
        assertEquals(date, captor.getValue().getPaymentDate());
        assertEquals("Dog Chow", captor.getValue().getProducts().get(0).getProduct().getName());
    }

    @Test
    void createPayment_whenDateIsNotInFuture_returns400() throws Exception {
        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson(LocalDate.now(), "2", "10.50")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void createPayment_whenProductLineInvalid_returns400() throws Exception {
        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson(LocalDate.now().plusDays(5), "0", "0")))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void createPayment_whenProductsEmpty_returns400() throws Exception {
        String json = """
            { "supplierId": 1, "paymentDate": "%s", "products": [] }
            """.formatted(LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void createPayment_whenSupplierNotExists_returns404() throws Exception {
        when(paymentService.createPayment(any(PaymentRequestDTO.class)))
            .thenThrow(new EntityNotFoundException("No encontrado"));

        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson(LocalDate.now().plusDays(5), "2", "10.50")))
            .andExpect(status().isNotFound());
    }

    // ---------- consultas ----------

    @Test
    void getAllPaymentsConditions_wrapsListInResponse() throws Exception {
        when(paymentService.getAllPaymentConditions())
            .thenReturn(List.of(new PaymentCondition(1, "Contado", "Pago inmediato")));

        mockMvc.perform(get("/api/payments/conditions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentConditions[0].name").value("Contado"));
    }

    @Test
    void getPaymentsBySupplierId_returns200() throws Exception {
        PaymentResponseDTO payment = new PaymentResponseDTO();
        payment.setPaymentId(7);
        when(paymentService.getAllPaymentsBySupplierId(1)).thenReturn(new SupplierPaymentsResponseDTO(1, List.of(payment)));

        mockMvc.perform(get("/api/payments/supplier/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supplierId").value(1))
            .andExpect(jsonPath("$.payments[0].paymentId").value(7));
    }

    @Test
    void getPaymentsBySupplierId_whenSupplierNotExists_returns404() throws Exception {
        when(paymentService.getAllPaymentsBySupplierId(999)).thenThrow(new EntityNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/payments/supplier/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getLastAndNextPaymentBySupplierId_returns200() throws Exception {
        PaymentResponseDTO next = new PaymentResponseDTO();
        next.setPaymentId(8);
        when(paymentService.getLastAndNextPaymentsBySupplierId(1))
            .thenReturn(new SupplierLastNextPaymentsResponseDTO(1, next, null));

        mockMvc.perform(get("/api/payments/supplier/1/last-and-next"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.next.paymentId").value(8))
            .andExpect(jsonPath("$.last").doesNotExist());
    }

    @Test
    void getUpcomingPaymentsAlerts_returns200WithAlerts() throws Exception {
        UpcomingPaymentAlertDTO alert = new UpcomingPaymentAlertDTO();
        alert.setPaymentId(7);
        alert.setDaysUntilPayment(3);
        when(paymentService.getUpcomingPayments()).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/payments/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].paymentId").value(7))
            .andExpect(jsonPath("$[0].daysUntilPayment").value(3));
    }
}
