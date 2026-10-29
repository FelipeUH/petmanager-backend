package com.codefactory.petmanager.g12.petmanager_backend.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.ProductDTO;
import com.codefactory.petmanager.g12.petmanager_backend.product.controller.dto.SupplierProductsResponse;
import com.codefactory.petmanager.g12.petmanager_backend.product.controller.dto.SuppliersProductsDTO;
import com.codefactory.petmanager.g12.petmanager_backend.product.mapper.ProductMapper;
import com.codefactory.petmanager.g12.petmanager_backend.product.model.Product;
import com.codefactory.petmanager.g12.petmanager_backend.product.model.SuppliersProducts;
import com.codefactory.petmanager.g12.petmanager_backend.product.repository.ProductRepository;
import com.codefactory.petmanager.g12.petmanager_backend.product.repository.SuppliersProductsRepository;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.repository.SupplierRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private SuppliersProductsRepository suppliersProductsRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;

    @InjectMocks private ProductService productService;

    private Supplier supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1);
        supplier.setName("Purina");

        product = new Product(10, "Dog Chow", "Purina", "Alimento para perro adulto");
    }

    // ---------- findProductsBySupplierId ----------

    @Test
    void findProductsBySupplierId_whenSupplierHasProducts_returnsThemWithRelationId() {
        SuppliersProducts relation = new SuppliersProducts(5, supplier, product);

        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(suppliersProductsRepository.findBySupplier(supplier)).thenReturn(List.of(relation));

        SupplierProductsResponse result = productService.findProductsBySupplierId(1);

        assertEquals(1, result.getSupplierId());
        assertEquals("Purina", result.getSupplierName());
        assertEquals(List.of(new SuppliersProductsDTO(5, product)), result.getSupplierProducts());
    }

    @Test
    void findProductsBySupplierId_whenSupplierHasNoProducts_returnsEmptyList() {
        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(suppliersProductsRepository.findBySupplier(supplier)).thenReturn(List.of());

        SupplierProductsResponse result = productService.findProductsBySupplierId(1);

        assertEquals(1, result.getSupplierId());
        assertEquals("Purina", result.getSupplierName());
        assertTrue(result.getSupplierProducts().isEmpty());
    }

    @Test
    void findProductsBySupplierId_whenSupplierNotExists_throwsEntityNotFound() {
        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> productService.findProductsBySupplierId(999));

        assertTrue(ex.getMessage().contains("999"));
        verifyNoInteractions(suppliersProductsRepository);
    }

    // ---------- findAllProducts ----------

    @Test
    void findAllProducts_returnsRepositoryResult() {
        List<Product> products = List.of(product);
        when(productRepository.findAll()).thenReturn(products);

        assertSame(products, productService.findAllProducts());
    }

    // ---------- findOrCreateProduct ----------

    @Test
    void findOrCreateProduct_whenProductExists_returnsExistingWithoutSaving() {
        ProductDTO dto = new ProductDTO("dog chow", "PURINA", null);
        Product mapped = new Product(0, "dog chow", "PURINA", null);

        when(productMapper.productDTOToProduct(dto)).thenReturn(mapped);
        when(productRepository.existsByNameIgnoreCaseAndBrandIgnoreCase("dog chow", "PURINA")).thenReturn(true);
        when(productRepository.findByNameIgnoreCaseAndBrandIgnoreCase("dog chow", "PURINA")).thenReturn(product);

        Product result = productService.findOrCreateProduct(dto);

        assertSame(product, result);
        verify(productRepository, never()).save(any());
    }

    @Test
    void findOrCreateProduct_whenProductNotExists_savesAndReturnsNewProduct() {
        ProductDTO dto = new ProductDTO("Cat Chow", "Purina", "Alimento para gato");
        Product mapped = new Product(0, "Cat Chow", "Purina", "Alimento para gato");
        Product saved = new Product(11, "Cat Chow", "Purina", "Alimento para gato");

        when(productMapper.productDTOToProduct(dto)).thenReturn(mapped);
        when(productRepository.existsByNameIgnoreCaseAndBrandIgnoreCase("Cat Chow", "Purina")).thenReturn(false);
        when(productRepository.save(mapped)).thenReturn(saved);

        Product result = productService.findOrCreateProduct(dto);

        assertSame(saved, result);
    }

    // ---------- addSupplierProduct ----------

    @Test
    void addSupplierProduct_whenSupplierAndProductExist_savesRelation() {
        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(suppliersProductsRepository.save(any(SuppliersProducts.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SuppliersProducts result = productService.addSupplierProduct(1, 10);

        ArgumentCaptor<SuppliersProducts> captor = ArgumentCaptor.forClass(SuppliersProducts.class);
        verify(suppliersProductsRepository).save(captor.capture());
        assertSame(supplier, captor.getValue().getSupplier());
        assertSame(product, captor.getValue().getProduct());
        assertSame(captor.getValue(), result);
    }

    @Test
    void addSupplierProduct_whenSupplierNotExists_throwsEntityNotFound() {
        when(supplierRepository.findById(999)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> productService.addSupplierProduct(999, 10));

        assertTrue(ex.getMessage().contains("999"));
        verify(productRepository, never()).findById(anyInt());
        verifyNoInteractions(suppliersProductsRepository);
    }

    @Test
    void addSupplierProduct_whenProductNotExists_throwsEntityNotFound() {
        when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> productService.addSupplierProduct(1, 999));

        assertTrue(ex.getMessage().contains("999"));
        verifyNoInteractions(suppliersProductsRepository);
    }

    // ---------- deleteSupplierProduct ----------

    @Test
    void deleteSupplierProduct_whenRelationExists_deletesIt() {
        when(suppliersProductsRepository.existsById(5)).thenReturn(true);

        productService.deleteSupplierProduct(5);

        verify(suppliersProductsRepository).deleteById(5);
    }

    @Test
    void deleteSupplierProduct_whenRelationNotExists_throwsEntityNotFound() {
        when(suppliersProductsRepository.existsById(999)).thenReturn(false);

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> productService.deleteSupplierProduct(999));

        assertTrue(ex.getMessage().contains("999"));
        verify(suppliersProductsRepository, never()).deleteById(anyInt());
    }
}
