package com.codefactory.petmanager.g12.petmanager_backend.supplier.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.codefactory.petmanager.g12.petmanager_backend.payment.model.PaymentCondition;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;

class SupplierMapperTest {

    private final SupplierMapper mapper = new SupplierMapperImpl();

    @Test
    void supplierToSupplierResponseDTO_copiesAllFields() {
        PaymentCondition condition = new PaymentCondition(2, "Crédito", null);
        Supplier supplier = new Supplier(1, "900123456", "Purina", "3001234567",
            "Calle 10 # 20-30", "contacto@purina.com", condition, "Notas");

        SupplierResponseDTO dto = mapper.supplierToSupplierResponseDTO(supplier);

        assertEquals(new SupplierResponseDTO(1, "900123456", "Purina", "3001234567",
            "Calle 10 # 20-30", "contacto@purina.com", condition, "Notas"), dto);
    }

    @Test
    void supplierRequestDTOToSupplier_copiesFieldsAndIgnoresIdAndPaymentCondition() {
        SupplierRequestDTO request = new SupplierRequestDTO("900123456", "Purina", "3001234567",
            "Calle 10 # 20-30", "contacto@purina.com", 2, "Notas");

        Supplier supplier = mapper.supplierRequestDTOToSupplier(request);

        assertEquals(0, supplier.getId());
        assertEquals("900123456", supplier.getNit());
        assertEquals("Purina", supplier.getName());
        assertEquals("3001234567", supplier.getPhoneNumber());
        assertEquals("Calle 10 # 20-30", supplier.getAddress());
        assertEquals("contacto@purina.com", supplier.getEmail());
        assertEquals("Notas", supplier.getPaymentNotes());
        assertNull(supplier.getPaymentCondition());
    }

    @Test
    void suppliersToSupplierResponseDTOs_mapsEveryElement() {
        Supplier first = new Supplier();
        first.setId(1);
        Supplier second = new Supplier();
        second.setId(2);

        List<SupplierResponseDTO> dtos = mapper.suppliersToSupplierResponseDTOs(List.of(first, second));

        assertEquals(List.of(1, 2), dtos.stream().map(SupplierResponseDTO::getId).toList());
    }

    @Test
    void nullInputs_returnNull() {
        assertNull(mapper.supplierToSupplierResponseDTO(null));
        assertNull(mapper.supplierRequestDTOToSupplier(null));
        assertNull(mapper.suppliersToSupplierResponseDTOs(null));
    }
}
