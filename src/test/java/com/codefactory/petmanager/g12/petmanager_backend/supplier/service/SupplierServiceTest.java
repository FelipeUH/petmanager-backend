package com.codefactory.petmanager.g12.petmanager_backend.supplier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.codefactory.petmanager.g12.petmanager_backend.payment.model.PaymentCondition;
import com.codefactory.petmanager.g12.petmanager_backend.payment.repository.PaymentConditionRepository;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierUpdateDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.mapper.SupplierMapper;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.repository.SupplierRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock private SupplierMapper supplierMapper;
    @Mock private SupplierRepository supplierRepository;
    @Mock private PaymentConditionRepository paymentConditionRepository;

    @InjectMocks private SupplierService supplierService;

    private Supplier supplier;
    private PaymentCondition paymentCondition;

    @BeforeEach
    void setUp() {
        paymentCondition = new PaymentCondition(1, "Contado", "Pago inmediato");

        supplier = new Supplier();
        supplier.setId(1);
        supplier.setNit("900123456");
        supplier.setName("Purina");
        supplier.setPhoneNumber("3001234567");
        supplier.setAddress("Calle 10 # 20-30");
        supplier.setEmail("contacto@purina.com");
        supplier.setPaymentCondition(paymentCondition);
    }

    private Supplier otherSupplier(int id, String nit, String name) {
        Supplier other = new Supplier();
        other.setId(id);
        other.setNit(nit);
        other.setName(name);
        return other;
    }

    // ---------- getSupplierById ----------

    @Test
    void getSupplierById_whenSupplierExists_returnsDTO() {
        SupplierResponseDTO expected = new SupplierResponseDTO();
        expected.setId(1);

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(supplierMapper.supplierToSupplierResponseDTO(supplier)).thenReturn(expected);

        SupplierResponseDTO result = supplierService.getSupplierById(1);

        assertSame(expected, result);
    }

    @Test
    void getSupplierById_whenSupplierNotExists_throwsEntityNotFound() {
        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> supplierService.getSupplierById(999));

        assertTrue(ex.getMessage().contains("999"));
        verifyNoInteractions(supplierMapper);
    }

    // ---------- getSupplierByNit ----------

    @Test
    void getSupplierByNit_whenSupplierExists_returnsDTO() {
        SupplierResponseDTO expected = new SupplierResponseDTO();

        when(supplierRepository.findByNit("900123456")).thenReturn(Optional.of(supplier));
        when(supplierMapper.supplierToSupplierResponseDTO(supplier)).thenReturn(expected);

        SupplierResponseDTO result = supplierService.getSupplierByNit("900123456");

        assertSame(expected, result);
    }

    @Test
    void getSupplierByNit_whenSupplierNotExists_throwsEntityNotFound() {
        when(supplierRepository.findByNit("000000000")).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> supplierService.getSupplierByNit("000000000"));

        assertTrue(ex.getMessage().contains("000000000"));
        verifyNoInteractions(supplierMapper);
    }

    // ---------- getAllSuppliers / searchSuppliersByName ----------

    @Test
    void getAllSuppliers_returnsMappedList() {
        List<Supplier> suppliers = List.of(supplier);
        List<SupplierResponseDTO> expected = List.of(new SupplierResponseDTO());

        when(supplierRepository.findAll()).thenReturn(suppliers);
        when(supplierMapper.suppliersToSupplierResponseDTOs(suppliers)).thenReturn(expected);

        List<SupplierResponseDTO> result = supplierService.getAllSuppliers();

        assertSame(expected, result);
    }

    @Test
    void searchSuppliersByName_returnsMappedList() {
        List<Supplier> suppliers = List.of(supplier);
        List<SupplierResponseDTO> expected = List.of(new SupplierResponseDTO());

        when(supplierRepository.findByNameContainingIgnoreCase("pur")).thenReturn(suppliers);
        when(supplierMapper.suppliersToSupplierResponseDTOs(suppliers)).thenReturn(expected);

        List<SupplierResponseDTO> result = supplierService.searchSuppliersByName("pur");

        assertSame(expected, result);
    }

    // ---------- searchSuppliers ----------

    @Test
    @SuppressWarnings("unchecked")
    void searchSuppliers_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Supplier> page = new PageImpl<>(List.of(supplier), pageable, 1);
        SupplierResponseDTO dto = new SupplierResponseDTO();

        when(supplierRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(supplierMapper.supplierToSupplierResponseDTO(supplier)).thenReturn(dto);

        Page<SupplierResponseDTO> result = supplierService.searchSuppliers("pur", null, null, 1, pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(dto, result.getContent().get(0));
    }

    // ---------- createSupplier ----------

    private SupplierRequestDTO buildRequest(Integer paymentConditionId) {
        return new SupplierRequestDTO("900123456", "Purina", "3001234567",
            "Calle 10 # 20-30", "contacto@purina.com", paymentConditionId, null);
    }

    @Test
    void createSupplier_whenValid_savesWithPaymentConditionAndReturnsDTO() {
        SupplierRequestDTO request = buildRequest(1);
        Supplier mapped = new Supplier();
        SupplierResponseDTO expected = new SupplierResponseDTO();

        when(supplierRepository.findByNit(request.getNit())).thenReturn(Optional.empty());
        when(supplierRepository.findByName(request.getName())).thenReturn(Optional.empty());
        when(paymentConditionRepository.findById(1)).thenReturn(Optional.of(paymentCondition));
        when(supplierMapper.supplierRequestDTOToSupplier(request)).thenReturn(mapped);
        when(supplierRepository.save(mapped)).thenReturn(supplier);
        when(supplierMapper.supplierToSupplierResponseDTO(supplier)).thenReturn(expected);

        SupplierResponseDTO result = supplierService.createSupplier(request);

        assertSame(expected, result);
        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertSame(paymentCondition, captor.getValue().getPaymentCondition());
    }

    @Test
    void createSupplier_whenPaymentConditionIdIsNull_usesDefaultPaymentCondition() {
        SupplierRequestDTO request = buildRequest(null);
        Supplier mapped = new Supplier();

        when(supplierRepository.findByNit(request.getNit())).thenReturn(Optional.empty());
        when(supplierRepository.findByName(request.getName())).thenReturn(Optional.empty());
        when(paymentConditionRepository.findById(1)).thenReturn(Optional.of(paymentCondition));
        when(supplierMapper.supplierRequestDTOToSupplier(request)).thenReturn(mapped);
        when(supplierRepository.save(mapped)).thenReturn(supplier);

        supplierService.createSupplier(request);

        verify(paymentConditionRepository).findById(1);
        assertSame(paymentCondition, mapped.getPaymentCondition());
    }

    @Test
    void createSupplier_whenNitAlreadyExists_throwsIllegalArgument() {
        SupplierRequestDTO request = buildRequest(1);

        when(supplierRepository.findByNit(request.getNit()))
            .thenReturn(Optional.of(otherSupplier(2, request.getNit(), "Otro")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> supplierService.createSupplier(request));

        assertTrue(ex.getMessage().contains(request.getNit()));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void createSupplier_whenNameAlreadyExists_throwsIllegalArgument() {
        SupplierRequestDTO request = buildRequest(1);

        when(supplierRepository.findByNit(request.getNit())).thenReturn(Optional.empty());
        when(supplierRepository.findByName(request.getName()))
            .thenReturn(Optional.of(otherSupplier(2, "111111111", request.getName())));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> supplierService.createSupplier(request));

        assertTrue(ex.getMessage().contains(request.getName()));
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void createSupplier_whenPaymentConditionNotExists_throwsEntityNotFound() {
        SupplierRequestDTO request = buildRequest(99);

        when(supplierRepository.findByNit(request.getNit())).thenReturn(Optional.empty());
        when(supplierRepository.findByName(request.getName())).thenReturn(Optional.empty());
        when(paymentConditionRepository.findById(99)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> supplierService.createSupplier(request));

        assertTrue(ex.getMessage().contains("99"));
        verify(supplierRepository, never()).save(any());
    }

    // ---------- updateSupplier ----------

    @Test
    void updateSupplier_whenSupplierNotExists_throwsEntityNotFound() {
        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
            () -> supplierService.updateSupplier(999, new SupplierUpdateDTO()));

        verify(supplierRepository, never()).save(any());
    }

    @Test
    void updateSupplier_whenOnlySomeFieldsProvided_updatesOnlyThoseFields() {
        SupplierUpdateDTO update = new SupplierUpdateDTO();
        update.setPhoneNumber("3109876543");
        update.setPaymentNotes("Pagar los viernes");
        SupplierResponseDTO expected = new SupplierResponseDTO();

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(supplier)).thenReturn(supplier);
        when(supplierMapper.supplierToSupplierResponseDTO(supplier)).thenReturn(expected);

        SupplierResponseDTO result = supplierService.updateSupplier(1, update);

        assertSame(expected, result);
        assertEquals("3109876543", supplier.getPhoneNumber());
        assertEquals("Pagar los viernes", supplier.getPaymentNotes());
        assertEquals("900123456", supplier.getNit());
        assertEquals("Purina", supplier.getName());
        assertEquals("Calle 10 # 20-30", supplier.getAddress());
        assertSame(paymentCondition, supplier.getPaymentCondition());
        verify(supplierRepository, never()).findByNit(anyString());
        verify(supplierRepository, never()).findByName(anyString());
        verifyNoInteractions(paymentConditionRepository);
    }

    @Test
    void updateSupplier_whenAllFieldsProvided_updatesAllFields() {
        PaymentCondition newCondition = new PaymentCondition(2, "Crédito 30 días", null);
        SupplierUpdateDTO update = new SupplierUpdateDTO("800999888", "Purina Colombia",
            "3109876543", "Carrera 50 # 10-20", null, 2, "Notas nuevas");

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByNit("800999888")).thenReturn(Optional.empty());
        when(supplierRepository.findByName("Purina Colombia")).thenReturn(Optional.empty());
        when(paymentConditionRepository.findById(2)).thenReturn(Optional.of(newCondition));
        when(supplierRepository.save(supplier)).thenReturn(supplier);

        supplierService.updateSupplier(1, update);

        assertEquals("800999888", supplier.getNit());
        assertEquals("Purina Colombia", supplier.getName());
        assertEquals("3109876543", supplier.getPhoneNumber());
        assertEquals("Carrera 50 # 10-20", supplier.getAddress());
        assertEquals("Notas nuevas", supplier.getPaymentNotes());
        assertSame(newCondition, supplier.getPaymentCondition());
    }

    @Test
    void updateSupplier_whenNitAndNameBelongToSameSupplier_doesNotThrow() {
        SupplierUpdateDTO update = new SupplierUpdateDTO();
        update.setNit(supplier.getNit());
        update.setName(supplier.getName());

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByNit(supplier.getNit())).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByName(supplier.getName())).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(supplier)).thenReturn(supplier);

        supplierService.updateSupplier(1, update);

        verify(supplierRepository).save(supplier);
    }

    @Test
    void updateSupplier_whenNitBelongsToAnotherSupplier_throwsIllegalArgument() {
        SupplierUpdateDTO update = new SupplierUpdateDTO();
        update.setNit("111111111");

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByNit("111111111"))
            .thenReturn(Optional.of(otherSupplier(2, "111111111", "Otro")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> supplierService.updateSupplier(1, update));

        assertTrue(ex.getMessage().contains("111111111"));
        assertEquals("900123456", supplier.getNit());
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void updateSupplier_whenNameBelongsToAnotherSupplier_throwsIllegalArgument() {
        SupplierUpdateDTO update = new SupplierUpdateDTO();
        update.setName("Otro");

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByName("Otro"))
            .thenReturn(Optional.of(otherSupplier(2, "111111111", "Otro")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> supplierService.updateSupplier(1, update));

        assertTrue(ex.getMessage().contains("Otro"));
        assertEquals("Purina", supplier.getName());
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void updateSupplier_whenPaymentConditionNotExists_throwsEntityNotFound() {
        SupplierUpdateDTO update = new SupplierUpdateDTO();
        update.setPaymentConditionId(99);

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(paymentConditionRepository.findById(99)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> supplierService.updateSupplier(1, update));

        assertTrue(ex.getMessage().contains("99"));
        verify(supplierRepository, never()).save(any());
    }

    // ---------- deleteSupplier ----------

    @Test
    void deleteSupplier_whenSupplierExists_deletesIt() {
        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));

        supplierService.deleteSupplier(1);

        verify(supplierRepository).delete(supplier);
    }

    @Test
    void deleteSupplier_whenSupplierNotExists_throwsEntityNotFound() {
        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
            () -> supplierService.deleteSupplier(999));

        verify(supplierRepository, never()).delete(any(Supplier.class));
    }

    // ---------- existsByNit / existsByName ----------

    @Test
    void existsByNit_delegatesToRepository() {
        when(supplierRepository.existsByNit("900123456")).thenReturn(true);
        when(supplierRepository.existsByNit("000000000")).thenReturn(false);

        assertTrue(supplierService.existsByNit("900123456"));
        assertFalse(supplierService.existsByNit("000000000"));
    }

    @Test
    void existsByName_whenSupplierExists_returnsTrue() {
        when(supplierRepository.findByName("Purina")).thenReturn(Optional.of(supplier));

        assertTrue(supplierService.existsByName("Purina"));
    }

    @Test
    void existsByName_whenSupplierNotExists_returnsFalse() {
        when(supplierRepository.findByName("Nadie")).thenReturn(Optional.empty());

        assertFalse(supplierService.existsByName("Nadie"));
    }
}
