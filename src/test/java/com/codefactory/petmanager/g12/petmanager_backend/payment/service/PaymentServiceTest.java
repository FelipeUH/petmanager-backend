package com.codefactory.petmanager.g12.petmanager_backend.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.PaymentRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.PaymentResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.PaymentsProductsDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.ProductDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.SupplierLastNextPaymentsResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.SupplierPaymentsResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.UpcomingPaymentAlertDTO;
import com.codefactory.petmanager.g12.petmanager_backend.payment.mapper.PaymentMapper;
import com.codefactory.petmanager.g12.petmanager_backend.payment.mapper.PaymentsProductsMapper;
import com.codefactory.petmanager.g12.petmanager_backend.payment.model.Payment;
import com.codefactory.petmanager.g12.petmanager_backend.payment.model.PaymentCondition;
import com.codefactory.petmanager.g12.petmanager_backend.payment.model.PaymentsProducts;
import com.codefactory.petmanager.g12.petmanager_backend.payment.repository.PaymentConditionRepository;
import com.codefactory.petmanager.g12.petmanager_backend.payment.repository.PaymentRepository;
import com.codefactory.petmanager.g12.petmanager_backend.payment.repository.PaymentsProductsRepository;
import com.codefactory.petmanager.g12.petmanager_backend.product.mapper.ProductMapper;
import com.codefactory.petmanager.g12.petmanager_backend.product.model.Product;
import com.codefactory.petmanager.g12.petmanager_backend.product.service.ProductService;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.repository.SupplierRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private PaymentsProductsRepository paymentsProductsRepository;
    @Mock private PaymentConditionRepository paymentConditionRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private ProductMapper productMapper;
    @Mock private PaymentsProductsMapper paymentsProductsMapper;
    @Mock private ProductService productService;

    @InjectMocks private PaymentService paymentService;

    private PaymentCondition paymentCondition;
    private Supplier supplier;
    private Product dogFood;
    private Product catFood;

    @BeforeEach
    void setUp() {
        paymentCondition = new PaymentCondition(1, "Contado", "Pago inmediato");

        supplier = new Supplier();
        supplier.setId(1);
        supplier.setName("Purina");
        supplier.setPaymentCondition(paymentCondition);

        dogFood = new Product(10, "Dog Chow", "Purina", null);
        catFood = new Product(11, "Cat Chow", "Purina", null);
    }

    private Payment buildPayment(int id, LocalDate date, Product... products) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setSupplier(supplier);
        payment.setPaymentDate(date);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPaymentsProducts(List.of(products).stream()
            .map(p -> new PaymentsProducts(0, payment, p, 1, new BigDecimal("100.00"), new BigDecimal("100.00")))
            .toList());
        return payment;
    }

    // ---------- createPayment ----------

    @Test
    void createPayment_whenValid_calculatesAmountsAndSavesPaymentWithProducts() {
        ProductDTO dogDTO = new ProductDTO("Dog Chow", "Purina", null);
        ProductDTO catDTO = new ProductDTO("Cat Chow", "Purina", null);
        LocalDate date = LocalDate.now().plusDays(5);
        PaymentRequestDTO request = new PaymentRequestDTO(1, date, List.of(
            new PaymentsProductsDTO(dogDTO, 2, new BigDecimal("10.50")),
            new PaymentsProductsDTO(catDTO, 3, new BigDecimal("5.00"))
        ), "Pago mensual");

        PaymentResponseDTO response = new PaymentResponseDTO();
        List<PaymentsProductsDTO> mappedProducts = List.of(new PaymentsProductsDTO());

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productService.findOrCreateProduct(dogDTO)).thenReturn(dogFood);
        when(productService.findOrCreateProduct(catDTO)).thenReturn(catFood);
        when(paymentsProductsRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentsProductsMapper.paymentsProductsListToPaymentsProductsDTOList(anyList())).thenReturn(mappedProducts);
        when(paymentMapper.paymentToPaymentResponseDTO(any(Payment.class))).thenReturn(response);

        PaymentResponseDTO result = paymentService.createPayment(request);

        assertSame(response, result);
        assertSame(mappedProducts, result.getProducts());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertSame(supplier, savedPayment.getSupplier());
        assertEquals(date, savedPayment.getPaymentDate());
        assertEquals("Pago mensual", savedPayment.getNotes());
        // 2 * 10.50 + 3 * 5.00 = 36.00
        assertEquals(0, new BigDecimal("36.00").compareTo(savedPayment.getAmount()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PaymentsProducts>> productsCaptor = ArgumentCaptor.forClass(List.class);
        verify(paymentsProductsRepository).saveAll(productsCaptor.capture());
        List<PaymentsProducts> savedProducts = productsCaptor.getValue();
        assertEquals(2, savedProducts.size());

        PaymentsProducts dogLine = savedProducts.get(0);
        assertSame(savedPayment, dogLine.getPayment());
        assertSame(dogFood, dogLine.getProduct());
        assertEquals(2, dogLine.getQuantity());
        assertEquals(0, new BigDecimal("10.50").compareTo(dogLine.getPricePerUnit()));
        assertEquals(0, new BigDecimal("21.00").compareTo(dogLine.getTotalAmount()));

        PaymentsProducts catLine = savedProducts.get(1);
        assertSame(catFood, catLine.getProduct());
        assertEquals(0, new BigDecimal("15.00").compareTo(catLine.getTotalAmount()));
    }

    @Test
    void createPayment_whenSupplierNotExists_throwsEntityNotFound() {
        PaymentRequestDTO request = new PaymentRequestDTO(999, LocalDate.now().plusDays(1), List.of(), null);
        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> paymentService.createPayment(request));

        assertTrue(ex.getMessage().contains("999"));
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(paymentsProductsRepository, productService);
    }

    // ---------- getAllPaymentConditions ----------

    @Test
    void getAllPaymentConditions_returnsRepositoryResult() {
        List<PaymentCondition> conditions = List.of(paymentCondition);
        when(paymentConditionRepository.findAll()).thenReturn(conditions);

        assertSame(conditions, paymentService.getAllPaymentConditions());
    }

    // ---------- getAllPaymentsBySupplierId ----------

    @Test
    void getAllPaymentsBySupplierId_returnsEveryPaymentWithItsProducts() {
        Payment first = buildPayment(1, LocalDate.now().minusDays(10), dogFood);
        Payment second = buildPayment(2, LocalDate.now().plusDays(10), catFood);
        PaymentResponseDTO firstDTO = new PaymentResponseDTO();
        PaymentResponseDTO secondDTO = new PaymentResponseDTO();
        List<PaymentsProductsDTO> firstProducts = List.of(new PaymentsProductsDTO());
        List<PaymentsProductsDTO> secondProducts = List.of(new PaymentsProductsDTO());

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(paymentRepository.findBySupplier(supplier)).thenReturn(List.of(first, second));
        when(paymentMapper.paymentToPaymentResponseDTO(first)).thenReturn(firstDTO);
        when(paymentMapper.paymentToPaymentResponseDTO(second)).thenReturn(secondDTO);
        when(paymentsProductsMapper.paymentsProductsListToPaymentsProductsDTOList(first.getPaymentsProducts()))
            .thenReturn(firstProducts);
        when(paymentsProductsMapper.paymentsProductsListToPaymentsProductsDTOList(second.getPaymentsProducts()))
            .thenReturn(secondProducts);

        SupplierPaymentsResponseDTO result = paymentService.getAllPaymentsBySupplierId(1);

        assertEquals(1, result.getSupplierId());
        assertEquals(2, result.getPayments().size());
        assertSame(firstDTO, result.getPayments().get(0));
        assertSame(firstProducts, firstDTO.getProducts());
        assertSame(secondDTO, result.getPayments().get(1));
        assertSame(secondProducts, secondDTO.getProducts());
    }

    @Test
    void getAllPaymentsBySupplierId_whenSupplierNotExists_throwsEntityNotFound() {
        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
            () -> paymentService.getAllPaymentsBySupplierId(999));

        verifyNoInteractions(paymentRepository);
    }

    // ---------- getLastAndNextPaymentsBySupplierId ----------

    @Test
    void getLastAndNextPaymentsBySupplierId_whenBothExist_returnsBoth() {
        Payment last = buildPayment(1, LocalDate.now().minusDays(3), dogFood);
        Payment next = buildPayment(2, LocalDate.now().plusDays(3), catFood);
        PaymentResponseDTO lastDTO = new PaymentResponseDTO();
        PaymentResponseDTO nextDTO = new PaymentResponseDTO();

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(paymentRepository.findTopBySupplierAndPaymentDateLessThanEqualOrderByPaymentDateDesc(eq(supplier), any(LocalDate.class)))
            .thenReturn(Optional.of(last));
        when(paymentRepository.findTopBySupplierAndPaymentDateAfterOrderByPaymentDateAsc(eq(supplier), any(LocalDate.class)))
            .thenReturn(Optional.of(next));
        when(paymentMapper.paymentToPaymentResponseDTO(last)).thenReturn(lastDTO);
        when(paymentMapper.paymentToPaymentResponseDTO(next)).thenReturn(nextDTO);

        SupplierLastNextPaymentsResponseDTO result = paymentService.getLastAndNextPaymentsBySupplierId(1);

        assertEquals(1, result.getSupplierId());
        assertSame(lastDTO, result.getLast());
        assertSame(nextDTO, result.getNext());
    }

    @Test
    void getLastAndNextPaymentsBySupplierId_whenNoPayments_returnsNulls() {
        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(paymentRepository.findTopBySupplierAndPaymentDateLessThanEqualOrderByPaymentDateDesc(eq(supplier), any(LocalDate.class)))
            .thenReturn(Optional.empty());
        when(paymentRepository.findTopBySupplierAndPaymentDateAfterOrderByPaymentDateAsc(eq(supplier), any(LocalDate.class)))
            .thenReturn(Optional.empty());

        SupplierLastNextPaymentsResponseDTO result = paymentService.getLastAndNextPaymentsBySupplierId(1);

        assertEquals(1, result.getSupplierId());
        assertNull(result.getLast());
        assertNull(result.getNext());
        verifyNoInteractions(paymentMapper);
    }

    @Test
    void getLastAndNextPaymentsBySupplierId_whenSupplierNotExists_throwsEntityNotFound() {
        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
            () -> paymentService.getLastAndNextPaymentsBySupplierId(999));

        verifyNoInteractions(paymentRepository);
    }

    // ---------- getUpcomingPayments ----------

    @Test
    void getUpcomingPayments_searchesNextTenDaysAndBuildsAlerts() {
        Payment upcoming = buildPayment(7, LocalDate.now().plusDays(3), dogFood, catFood);
        ProductDTO dogDTO = new ProductDTO("Dog Chow", "Purina", null);
        ProductDTO catDTO = new ProductDTO("Cat Chow", "Purina", null);

        when(paymentRepository.findByPaymentDateBetween(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(upcoming));
        when(productMapper.productToProductDTO(dogFood)).thenReturn(dogDTO);
        when(productMapper.productToProductDTO(catFood)).thenReturn(catDTO);

        List<UpcomingPaymentAlertDTO> result = paymentService.getUpcomingPayments();

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(paymentRepository).findByPaymentDateBetween(from.capture(), to.capture());
        assertEquals(10, ChronoUnit.DAYS.between(from.getValue(), to.getValue()));

        assertEquals(1, result.size());
        UpcomingPaymentAlertDTO alert = result.get(0);
        assertEquals(7, alert.getPaymentId());
        assertEquals(1, alert.getSupplierId());
        assertEquals("Purina", alert.getSupplierName());
        assertEquals(upcoming.getPaymentDate(), alert.getPaymentDate());
        assertEquals(3, alert.getDaysUntilPayment());
        assertEquals(upcoming.getAmount(), alert.getAmount());
        assertSame(paymentCondition, alert.getPaymentCondition());
        assertEquals(List.of(dogDTO, catDTO), alert.getProvidedProducts());
    }

    @Test
    void getUpcomingPayments_whenNoPayments_returnsEmptyList() {
        when(paymentRepository.findByPaymentDateBetween(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of());

        assertTrue(paymentService.getUpcomingPayments().isEmpty());
    }
}
