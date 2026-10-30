package com.codefactory.petmanager.g12.petmanager_backend.supplier.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.codefactory.petmanager.g12.petmanager_backend.common.exceptions.GlobalExceptionHandler;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierUpdateDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.service.SupplierService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios del controlador con MockMvc standalone: cubren rutas, códigos HTTP,
 * validación y manejo de errores. Las reglas de @PreAuthorize no se aplican aquí.
 */
@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

    private static final String VALID_SUPPLIER_JSON = """
        {
          "nit": "900123456",
          "name": "Purina",
          "phoneNumber": "3001234567",
          "address": "Calle 10 # 20-30",
          "email": "contacto@purina.com"
        }
        """;

    @Mock private SupplierService supplierService;

    @InjectMocks private SupplierController supplierController;

    private MockMvc mockMvc;
    private SupplierResponseDTO purina;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(supplierController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        purina = new SupplierResponseDTO();
        purina.setId(1);
        purina.setNit("900123456");
        purina.setName("Purina");
    }

    // ---------- consultas ----------

    @Test
    void getAllSuppliers_returns200WithList() throws Exception {
        when(supplierService.getAllSuppliers()).thenReturn(List.of(purina));

        mockMvc.perform(get("/api/suppliers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Purina"));
    }

    @Test
    void getSupplier_whenExists_returns200() throws Exception {
        when(supplierService.getSupplierById(1)).thenReturn(purina);

        mockMvc.perform(get("/api/suppliers/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nit").value("900123456"));
    }

    @Test
    void getSupplier_whenNotExists_returns404WithMessage() throws Exception {
        when(supplierService.getSupplierById(999)).thenThrow(new EntityNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/suppliers/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("No encontrado"))
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getSupplier_whenIdNotNumeric_returns400() throws Exception {
        mockMvc.perform(get("/api/suppliers/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("El parámetro 'id' tiene un valor inválido: abc"));

        verifyNoInteractions(supplierService);
    }

    @Test
    void getSupplierByNit_returns200() throws Exception {
        when(supplierService.getSupplierByNit("900123456")).thenReturn(purina);

        mockMvc.perform(get("/api/suppliers/nit/900123456"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    // ---------- búsqueda paginada ----------

    @Test
    void searchSuppliers_withoutParams_usesDefaultPagination() throws Exception {
        when(supplierService.searchSuppliers(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(purina), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/suppliers/search"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Purina"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(supplierService).searchSuppliers(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "id"), captor.getValue().getSort());
    }

    @Test
    void searchSuppliers_withFiltersAndSort_passesThemToService() throws Exception {
        when(supplierService.searchSuppliers(eq("pur"), eq("900"), eq("calle"), eq(2), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/suppliers/search")
                .param("name", "pur").param("nit", "900").param("address", "calle")
                .param("paymentConditionId", "2")
                .param("page", "1").param("size", "5").param("sort", "name,desc"))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(supplierService).searchSuppliers(eq("pur"), eq("900"), eq("calle"), eq(2), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "name"), captor.getValue().getSort());
    }

    @Test
    void searchSuppliers_whenSortHasNoDirection_defaultsToAsc() throws Exception {
        when(supplierService.searchSuppliers(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/suppliers/search").param("sort", "name"))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(supplierService).searchSuppliers(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertEquals(Sort.by(Sort.Direction.ASC, "name"), captor.getValue().getSort());
    }

    // ---------- crear ----------

    @Test
    void createSupplier_whenValid_returns201() throws Exception {
        when(supplierService.createSupplier(any(SupplierRequestDTO.class))).thenReturn(purina);

        mockMvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON).content(VALID_SUPPLIER_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));

        ArgumentCaptor<SupplierRequestDTO> captor = ArgumentCaptor.forClass(SupplierRequestDTO.class);
        verify(supplierService).createSupplier(captor.capture());
        assertEquals("900123456", captor.getValue().getNit());
        assertEquals("Purina", captor.getValue().getName());
    }

    @Test
    void createSupplier_whenBodyInvalid_returns400WithoutCallingService() throws Exception {
        String invalid = """
            { "nit": "12", "name": "Purina", "phoneNumber": "3001234567",
              "address": "Calle 10 # 20-30", "email": "no-es-email" }
            """;

        mockMvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON).content(invalid))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(supplierService);
    }

    @Test
    void createSupplier_whenBodyMissing_returns400() throws Exception {
        mockMvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Se requiere el cuerpo de la petición"));
    }

    @Test
    void createSupplier_whenDuplicated_returns400WithMessage() throws Exception {
        when(supplierService.createSupplier(any(SupplierRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("Un proveedor con NIT 900123456 ya existe!"));

        mockMvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON).content(VALID_SUPPLIER_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Un proveedor con NIT 900123456 ya existe!"));
    }

    // ---------- actualizar / eliminar ----------

    @Test
    void updateSupplier_whenValid_returns200() throws Exception {
        when(supplierService.updateSupplier(eq(1), any(SupplierUpdateDTO.class))).thenReturn(purina);

        mockMvc.perform(put("/api/suppliers/1").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"phoneNumber\": \"3109876543\" }"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateSupplier_whenBodyInvalid_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(put("/api/suppliers/1").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nit\": \"abc\" }"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(supplierService);
    }

    @Test
    void deleteSupplier_whenExists_returns204() throws Exception {
        mockMvc.perform(delete("/api/suppliers/1"))
            .andExpect(status().isNoContent());

        verify(supplierService).deleteSupplier(1);
    }

    @Test
    void deleteSupplier_whenNotExists_returns404() throws Exception {
        doThrow(new EntityNotFoundException("No encontrado")).when(supplierService).deleteSupplier(999);

        mockMvc.perform(delete("/api/suppliers/999"))
            .andExpect(status().isNotFound());
    }

    // ---------- existencia ----------

    @Test
    void existsByNit_returnsBoolean() throws Exception {
        when(supplierService.existsByNit("900123456")).thenReturn(true);

        mockMvc.perform(get("/api/suppliers/exists/nit/900123456"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
    }

    @Test
    void existsByName_returnsBoolean() throws Exception {
        when(supplierService.existsByName("Nadie")).thenReturn(false);

        mockMvc.perform(get("/api/suppliers/exists/name/Nadie"))
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }
}
