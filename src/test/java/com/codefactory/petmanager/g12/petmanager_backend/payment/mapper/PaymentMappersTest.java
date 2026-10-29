package com.codefactory.petmanager.g12.petmanager_backend.payment.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.PaymentRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.PaymentResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.PaymentsProductsDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.ProductDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.model.Payment;
import com.codefactory.petmanager.g12.petmanager_backend.payment.model.PaymentsProducts;
import com.codefactory.petmanager.g12.petmanager_backend.product.mapper.ProductMapperImpl;
import com.codefactory.petmanager.g12.petmanager_backend.product.model.Product;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;

class PaymentMappersTest {

    private final PaymentMapper paymentMapper = new PaymentMapperImpl();
    private PaymentsProductsMapper paymentsProductsMapper;

    @BeforeEach
    void setUp() {
        // En la app Spring inyecta ProductMapper; aquí se inyecta a mano la implementación real
        PaymentsProductsMapperImpl impl = new PaymentsProductsMapperImpl();
        ReflectionTestUtils.setField(impl, "productMapper", new ProductMapperImpl());
        paymentsProductsMapper = impl;
    }

    // ---------- PaymentMapper ----------

    @Test
    void paymentToPaymentResponseDTO_mapsIdToPaymentIdAndLeavesProductsEmpty() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        Payment payment = new Payment(7, new Supplier(), List.of(), date, new BigDecimal("36.00"), "Notas");

        PaymentResponseDTO dto = paymentMapper.paymentToPaymentResponseDTO(payment);

        assertEquals(new PaymentResponseDTO(7, date, new BigDecimal("36.00"), null, "Notas"), dto);
    }

    @Test
    void paymentRequestDTOToPayment_copiesDateAndNotesOnly() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        PaymentRequestDTO request = new PaymentRequestDTO(1, date, List.of(), "Notas");

        Payment payment = paymentMapper.paymentRequestDTOToPayment(request);

        assertEquals(date, payment.getPaymentDate());
        assertEquals("Notas", payment.getNotes());
        assertEquals(0, payment.getId());
        assertNull(payment.getSupplier());
        assertNull(payment.getAmount());
        assertNull(payment.getPaymentsProducts());
    }

    @Test
    void paymentsToPaymentResponseDTOs_mapsEveryElement() {
        Payment first = new Payment();
        first.setId(1);
        Payment second = new Payment();
        second.setId(2);

        List<PaymentResponseDTO> dtos = paymentMapper.paymentsToPaymentResponseDTOs(List.of(first, second));

        assertEquals(List.of(1, 2), dtos.stream().map(PaymentResponseDTO::getPaymentId).toList());
    }

    // ---------- PaymentsProductsMapper ----------

    @Test
    void paymentsProductsToPaymentsProductsDTO_mapsProductQuantityAndPrice() {
        Product product = new Product(10, "Dog Chow", "Purina", null);
        PaymentsProducts line = new PaymentsProducts(1, new Payment(), product, 2,
            new BigDecimal("10.50"), new BigDecimal("21.00"));

        PaymentsProductsDTO dto = paymentsProductsMapper.paymentsProductsToPaymentsProductsDTO(line);

        assertEquals(new PaymentsProductsDTO(new ProductDTO("Dog Chow", "Purina", null), 2,
            new BigDecimal("10.50")), dto);
    }

    @Test
    void paymentsProductsListToPaymentsProductsDTOList_mapsEveryElement() {
        PaymentsProducts first = new PaymentsProducts(1, null, new Product(1, "A", "X", null), 1, BigDecimal.ONE, BigDecimal.ONE);
        PaymentsProducts second = new PaymentsProducts(2, null, new Product(2, "B", "Y", null), 3, BigDecimal.TEN, BigDecimal.TEN);

        List<PaymentsProductsDTO> dtos = paymentsProductsMapper
            .paymentsProductsListToPaymentsProductsDTOList(List.of(first, second));

        assertEquals(List.of(1, 3), dtos.stream().map(PaymentsProductsDTO::getQuantity).toList());
        assertEquals("B", dtos.get(1).getProduct().getName());
    }

    @Test
    void nullInputs_returnNull() {
        assertNull(paymentMapper.paymentToPaymentResponseDTO(null));
        assertNull(paymentMapper.paymentRequestDTOToPayment(null));
        assertNull(paymentsProductsMapper.paymentsProductsToPaymentsProductsDTO(null));
        assertNull(paymentsProductsMapper.paymentsProductsListToPaymentsProductsDTOList(null));
    }
}
