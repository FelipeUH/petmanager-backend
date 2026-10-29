package com.codefactory.petmanager.g12.petmanager_backend.product.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.codefactory.petmanager.g12.petmanager_backend.payment.controller.dto.ProductDTO;
import com.codefactory.petmanager.g12.petmanager_backend.product.model.Product;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapperImpl();

    @Test
    void productToProductDTO_copiesNameBrandAndDescription() {
        Product product = new Product(10, "Dog Chow", "Purina", "Alimento");

        assertEquals(new ProductDTO("Dog Chow", "Purina", "Alimento"), mapper.productToProductDTO(product));
    }

    @Test
    void productDTOToProduct_copiesFieldsAndLeavesIdUnset() {
        Product product = mapper.productDTOToProduct(new ProductDTO("Dog Chow", "Purina", "Alimento"));

        assertEquals(new Product(0, "Dog Chow", "Purina", "Alimento"), product);
    }

    @Test
    void listMappings_mapEveryElement() {
        List<Product> products = List.of(new Product(1, "A", "X", null), new Product(2, "B", "Y", null));
        List<ProductDTO> dtos = List.of(new ProductDTO("A", "X", null), new ProductDTO("B", "Y", null));

        assertEquals(dtos, mapper.productsToProductDTOs(products));
        assertEquals(List.of(new Product(0, "A", "X", null), new Product(0, "B", "Y", null)),
            mapper.productDTOsToProducts(dtos));
    }

    @Test
    void nullInputs_returnNull() {
        assertNull(mapper.productToProductDTO(null));
        assertNull(mapper.productDTOToProduct(null));
        assertNull(mapper.productsToProductDTOs(null));
        assertNull(mapper.productDTOsToProducts(null));
    }
}
