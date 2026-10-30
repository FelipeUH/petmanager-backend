package com.codefactory.petmanager.g12.petmanager_backend.product.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.codefactory.petmanager.g12.petmanager_backend.common.exceptions.GlobalExceptionHandler;
import com.codefactory.petmanager.g12.petmanager_backend.product.controller.dto.SupplierProductsResponse;
import com.codefactory.petmanager.g12.petmanager_backend.product.controller.dto.SuppliersProductsDTO;
import com.codefactory.petmanager.g12.petmanager_backend.product.model.Product;
import com.codefactory.petmanager.g12.petmanager_backend.product.model.SuppliersProducts;
import com.codefactory.petmanager.g12.petmanager_backend.product.service.ProductService;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock private ProductService productService;

    @InjectMocks private ProductController productController;

    private MockMvc mockMvc;
    private Product dogFood;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        dogFood = new Product(10, "Dog Chow", "Purina", null);
    }

    @Test
    void getAllProducts_returns200WithList() throws Exception {
        when(productService.findAllProducts()).thenReturn(List.of(dogFood));

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].name").value("Dog Chow"));
    }

    @Test
    void getProductsBySupplier_returns200WithProducts() throws Exception {
        SupplierProductsResponse response =
            new SupplierProductsResponse(1, "Purina", List.of(new SuppliersProductsDTO(5, dogFood)));
        when(productService.findProductsBySupplierId(1)).thenReturn(response);

        mockMvc.perform(get("/api/products/supplier/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supplierName").value("Purina"))
            .andExpect(jsonPath("$.supplierProducts[0].supplierProductId").value(5))
            .andExpect(jsonPath("$.supplierProducts[0].product.name").value("Dog Chow"));
    }

    @Test
    void getProductsBySupplier_whenSupplierNotExists_returns404() throws Exception {
        when(productService.findProductsBySupplierId(999)).thenThrow(new EntityNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/products/supplier/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void addSupplierProduct_returns201WithRelation() throws Exception {
        Supplier supplier = new Supplier();
        supplier.setId(1);
        when(productService.addSupplierProduct(1, 10)).thenReturn(new SuppliersProducts(5, supplier, dogFood));

        mockMvc.perform(post("/api/products/supplier-product").param("supplierId", "1").param("productId", "10"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.product.id").value(10));
    }

    @Test
    void addSupplierProduct_whenParamMissing_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/products/supplier-product").param("supplierId", "1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("El parámetro 'productId' es requerido"));

        verifyNoInteractions(productService);
    }

    @Test
    void deleteSupplierProduct_returns204() throws Exception {
        mockMvc.perform(delete("/api/products/supplier-product/5"))
            .andExpect(status().isNoContent());

        verify(productService).deleteSupplierProduct(5);
    }

    @Test
    void deleteSupplierProduct_whenNotExists_returns404() throws Exception {
        doThrow(new EntityNotFoundException("No encontrado")).when(productService).deleteSupplierProduct(999);

        mockMvc.perform(delete("/api/products/supplier-product/999"))
            .andExpect(status().isNotFound());
    }
}
