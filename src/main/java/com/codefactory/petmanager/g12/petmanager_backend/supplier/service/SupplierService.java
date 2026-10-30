package com.codefactory.petmanager.g12.petmanager_backend.supplier.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codefactory.petmanager.g12.petmanager_backend.payment.model.PaymentCondition;
import com.codefactory.petmanager.g12.petmanager_backend.payment.repository.PaymentConditionRepository;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierUpdateDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.mapper.SupplierMapper;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.repository.SupplierRepository;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.repository.specification.SupplierSpecification;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierService {
    
  private final SupplierMapper supplierMapper;
  private final SupplierRepository supplierRepository;
  private final PaymentConditionRepository paymentConditionRepository;

  @Transactional(readOnly = true)
  public SupplierResponseDTO getSupplierById(int supplierId) {
      Supplier supplier = getSupplierOrThrow(supplierId);
      return supplierMapper.supplierToSupplierResponseDTO(supplier);
  }

  @Transactional(readOnly = true)
  public SupplierResponseDTO getSupplierByNit(String nit) {
      Supplier supplier = supplierRepository.findByNit(nit)
              .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado un proveedor con NIT: " + nit));
      return supplierMapper.supplierToSupplierResponseDTO(supplier);
  }

  @Transactional(readOnly = true)
  public List<SupplierResponseDTO> getAllSuppliers() {
      List<Supplier> suppliers = supplierRepository.findAll();
      return supplierMapper.suppliersToSupplierResponseDTOs(suppliers);
  }

  @Transactional(readOnly = true)
  public Page<SupplierResponseDTO> searchSuppliers(
    String name,
    String nit,
    String address,
    Integer paymentConditionId,
    Pageable pageable) {

    Specification<Supplier> spec = buildSpecification(name, nit, address, paymentConditionId);

    Page<Supplier> resultPage = supplierRepository.findAll(spec, pageable);
    return resultPage.map(supplierMapper::supplierToSupplierResponseDTO);
  }

  @Transactional(readOnly = true)
  public List<SupplierResponseDTO> searchSuppliersByName(String name) {
      List<Supplier> suppliers = supplierRepository.findByNameContainingIgnoreCase(name);
      return supplierMapper.suppliersToSupplierResponseDTOs(suppliers);
  }

  @Transactional
  public SupplierResponseDTO createSupplier(SupplierRequestDTO supplierRequestDTO) {
      validateUniqueNit(supplierRequestDTO.getNit(), null);
      validateUniqueName(supplierRequestDTO.getName(), null);

      int paymentConditionId = Optional.ofNullable(supplierRequestDTO.getPaymentConditionId()).orElse(1);
      PaymentCondition paymentCondition = getPaymentConditionOrThrow(paymentConditionId);
      
      Supplier supplier = supplierMapper.supplierRequestDTOToSupplier(supplierRequestDTO);
      supplier.setPaymentCondition(paymentCondition);

      Supplier savedSupplier = supplierRepository.save(supplier);
      return supplierMapper.supplierToSupplierResponseDTO(savedSupplier);
  }

  @Transactional
  public SupplierResponseDTO updateSupplier(int supplierId, SupplierUpdateDTO supplierUpdateDTO) {
      Supplier existingSupplier = getSupplierOrThrow(supplierId);
      
      if (supplierUpdateDTO.getNit() != null) {
          validateUniqueNit(supplierUpdateDTO.getNit(), supplierId);
          existingSupplier.setNit(supplierUpdateDTO.getNit());
      }
      
      if (supplierUpdateDTO.getName() != null) {
          validateUniqueName(supplierUpdateDTO.getName(), supplierId);
          existingSupplier.setName(supplierUpdateDTO.getName());
      }
      
      if (supplierUpdateDTO.getPhoneNumber() != null) existingSupplier.setPhoneNumber(supplierUpdateDTO.getPhoneNumber());
      if (supplierUpdateDTO.getAddress() != null) existingSupplier.setAddress(supplierUpdateDTO.getAddress());
      if (supplierUpdateDTO.getPaymentNotes() != null) existingSupplier.setPaymentNotes(supplierUpdateDTO.getPaymentNotes());
      if (supplierUpdateDTO.getEmail() != null) existingSupplier.setEmail(supplierUpdateDTO.getEmail());

      if (supplierUpdateDTO.getPaymentConditionId() != null) {
          existingSupplier.setPaymentCondition(getPaymentConditionOrThrow(supplierUpdateDTO.getPaymentConditionId()));
      }
      
      Supplier updatedSupplier = supplierRepository.save(existingSupplier);
      return supplierMapper.supplierToSupplierResponseDTO(updatedSupplier);
  }

  @Transactional
  public void deleteSupplier(int supplierId) {
      Supplier supplier = getSupplierOrThrow(supplierId);
      supplierRepository.delete(supplier);
  }

  @Transactional(readOnly = true)
  public boolean existsByNit(String nit) {
      return supplierRepository.existsByNit(nit);
  }

  @Transactional(readOnly = true)
  public boolean existsByName(String name) {
      return supplierRepository.findByName(name).isPresent();
  }

  private Supplier getSupplierOrThrow(int supplierId) {
      return supplierRepository.findById(supplierId)
              .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado un proveedor con id: " + supplierId));
  }

  private PaymentCondition getPaymentConditionOrThrow(int paymentConditionId) {
      return paymentConditionRepository.findById(paymentConditionId)
              .orElseThrow(() -> new EntityNotFoundException("No se ha encontrado una condición de pago con id: " + paymentConditionId));
  }

  private Specification<Supplier> buildSpecification(String name, String nit, String address, Integer paymentConditionId) {
    List<Specification<Supplier>> specs = new ArrayList<>();
    specs.add(SupplierSpecification.nameContains(name));
    specs.add(SupplierSpecification.nitContains(nit));
    specs.add(SupplierSpecification.addressContains(address));
    specs.add(SupplierSpecification.paymentConditionEquals(paymentConditionId));

    return specs.stream()
            .filter(Objects::nonNull)
            .reduce(Specification::and)
            .orElse(null);
  }

  private void validateUniqueNit(String nit, Integer excludeId) {
      supplierRepository.findByNit(nit).ifPresent(existing -> {
          if (excludeId == null || existing.getId() != excludeId) {
              throw new IllegalArgumentException("Un proveedor con NIT " + nit + " ya existe!");
          }
      });
  }

  private void validateUniqueName(String name, Integer excludeId) {
      supplierRepository.findByName(name).ifPresent(existing -> {
          if (excludeId == null || existing.getId() != excludeId) {
              throw new IllegalArgumentException("Un proveedor con el nombre " + name + " ya existe!");
          }
      });
  }
}