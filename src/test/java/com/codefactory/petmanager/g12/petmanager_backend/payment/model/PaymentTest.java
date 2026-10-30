package com.codefactory.petmanager.g12.petmanager_backend.payment.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codefactory.petmanager.g12.petmanager_backend.product.model.Product;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;

class PaymentTest {

    private Payment payment;
    private PaymentsProducts line;

    @BeforeEach
    void setUp() {
        // Relación bidireccional: el pago contiene la línea y la línea apunta al pago
        payment = new Payment();
        payment.setId(1);
        payment.setSupplier(new Supplier());
        payment.setPaymentDate(LocalDate.of(2026, 11, 15));
        payment.setAmount(new BigDecimal("21.00"));

        line = new PaymentsProducts(1, payment, new Product(10, "Dog Chow", "Purina", null),
            2, new BigDecimal("10.50"), new BigDecimal("21.00"));
        payment.setPaymentsProducts(List.of(line));
    }

    @Test
    void hashCode_withBidirectionalRelation_doesNotOverflow() {
        assertDoesNotThrow(payment::hashCode);
        assertDoesNotThrow(line::hashCode);
    }

    @Test
    void toString_withBidirectionalRelation_doesNotOverflow() {
        assertDoesNotThrow(payment::toString);
        assertDoesNotThrow(line::toString);
    }

    @Test
    void equals_ignoresPaymentsProducts() {
        Payment copy = new Payment(1, payment.getSupplier(), List.of(), payment.getPaymentDate(),
            payment.getAmount(), null);

        assertEquals(payment, copy);
        assertEquals(payment.hashCode(), copy.hashCode());
    }
}
