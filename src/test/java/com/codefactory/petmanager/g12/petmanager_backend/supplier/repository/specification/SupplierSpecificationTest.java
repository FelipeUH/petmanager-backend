package com.codefactory.petmanager.g12.petmanager_backend.supplier.repository.specification;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class SupplierSpecificationTest {

    @Mock private Root<Supplier> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;
    @Mock private Path<String> stringPath;
    @Mock private Path<Object> objectPath;
    @Mock private Expression<String> lowered;
    @Mock private Predicate predicate;

    private Predicate apply(Specification<Supplier> spec) {
        return spec.toPredicate(root, query, cb);
    }

    // ---------- filtros vacíos ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void textFilters_whenValueIsNullOrBlank_returnNullPredicate(String value) {
        assertNull(apply(SupplierSpecification.nameContains(value)));
        assertNull(apply(SupplierSpecification.nitContains(value)));
        assertNull(apply(SupplierSpecification.addressContains(value)));
        verifyNoInteractions(root, cb);
    }

    @Test
    void paymentConditionEquals_whenIdIsNull_returnsNullPredicate() {
        assertNull(apply(SupplierSpecification.paymentConditionEquals(null)));
        verifyNoInteractions(root, cb);
    }

    // ---------- filtros con valor ----------

    @Test
    void nameContains_buildsCaseInsensitiveLike() {
        when(root.<String>get("name")).thenReturn(stringPath);
        when(cb.lower(stringPath)).thenReturn(lowered);
        when(cb.like(lowered, "%purina%")).thenReturn(predicate);

        assertSame(predicate, apply(SupplierSpecification.nameContains("PuRiNa")));
    }

    @Test
    void nitContains_buildsLikeWithoutLowerCasing() {
        when(root.<String>get("nit")).thenReturn(stringPath);
        when(cb.like(stringPath, "%9001%")).thenReturn(predicate);

        assertSame(predicate, apply(SupplierSpecification.nitContains("9001")));
    }

    @Test
    void addressContains_buildsCaseInsensitiveLike() {
        when(root.<String>get("address")).thenReturn(stringPath);
        when(cb.lower(stringPath)).thenReturn(lowered);
        when(cb.like(lowered, "%calle 10%")).thenReturn(predicate);

        assertSame(predicate, apply(SupplierSpecification.addressContains("Calle 10")));
    }

    @Test
    void paymentConditionEquals_comparesNestedId() {
        when(root.get("paymentCondition")).thenReturn(objectPath);
        when(objectPath.get("id")).thenReturn(objectPath);
        when(cb.equal(objectPath, 2)).thenReturn(predicate);

        assertSame(predicate, apply(SupplierSpecification.paymentConditionEquals(2)));
    }
}
