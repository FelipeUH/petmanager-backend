package com.codefactory.petmanager.g12.petmanager_backend.supplier.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.codefactory.petmanager.g12.petmanager_backend.common.exceptions.dto.ErrorResponse;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.controller.dto.SupplierUpdateDTO;
import com.codefactory.petmanager.g12.petmanager_backend.supplier.service.SupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Proveedores", description = "Operaciones CRUD relacionadas a los proveedores")
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

  private final SupplierService supplierService;

  @Operation(summary = "Listar proveedores", description = "Devuelve todos los proveedores")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Operación exitosa",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = SupplierResponseDTO.class)))
  })
  @GetMapping
  public ResponseEntity<List<SupplierResponseDTO>> getAllSuppliers() {
    List<SupplierResponseDTO> suppliers = supplierService.getAllSuppliers();
    return ResponseEntity.ok(suppliers);
  }

  @Operation(summary = "Buscar proveedores con filtros y paginación", 
            description = "Permite filtrar por nombre, NIT, dirección o condición de pago, con resultados paginados.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Operación exitosa",
          content = @Content(mediaType = "application/json",
          schema = @Schema(implementation = SupplierResponseDTO.class)))
  })
  @GetMapping("/search")
  public ResponseEntity<Page<SupplierResponseDTO>> searchSuppliers(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String nit,
        @RequestParam(required = false) String address,
        @RequestParam(required = false) Integer paymentConditionId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id,asc") String[] sort) {

    Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

    Page<SupplierResponseDTO> result = supplierService.searchSuppliers(name, nit, address, paymentConditionId, pageable);
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "Obtener proveedor por ID", description = "Devuelve el proveedor que coincida con el ID proporcionado")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Proveedor encontrado",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = SupplierResponseDTO.class))),
    @ApiResponse(responseCode = "404", description = "Proveedor no encontrado",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/{id}")
  public ResponseEntity<SupplierResponseDTO> getSupplier(@PathVariable int id) {
    SupplierResponseDTO supplier = supplierService.getSupplierById(id);
    return ResponseEntity.ok(supplier);
  }

  @Operation(summary = "Obtener proveedor por NIT", description = "Devuelve el proveedor que coincida con el NIT proporcionado")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Proveedor encontrado",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = SupplierResponseDTO.class))),
    @ApiResponse(responseCode = "404", description = "Proveedor no encontrado",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/nit/{nit}")
  public ResponseEntity<SupplierResponseDTO> getSupplierByNit(@PathVariable String nit) {
    SupplierResponseDTO supplier = supplierService.getSupplierByNit(nit);
    return ResponseEntity.ok(supplier);
  }

  @Operation(summary = "Crear proveedor", description = "Crea un nuevo proveedor con los datos proporcionados. Necesario rol ADMIN o MANAGER.")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Proveedor creado",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = SupplierResponseDTO.class))),
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "409", description = "Proveedor duplicado",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @PostMapping
  public ResponseEntity<SupplierResponseDTO> createSupplier(@Valid @RequestBody SupplierRequestDTO supplierRequestDTO) {
    SupplierResponseDTO createdSupplier = supplierService.createSupplier(supplierRequestDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdSupplier);
  }

  @Operation(summary = "Actualizar proveedor", description = "Actualiza un proveedor existente que coincida con el ID proporcionado. Necesario rol ADMIN o MANAGER.")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Proveedor actualizado",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = SupplierResponseDTO.class))),
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Proveedor no encontrado",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @PutMapping("/{id}")
  public ResponseEntity<SupplierResponseDTO> updateSupplier(@PathVariable int id, @Valid @RequestBody SupplierUpdateDTO supplierUpdateDTO) {
    SupplierResponseDTO updatedSupplier = supplierService.updateSupplier(id, supplierUpdateDTO);
    return ResponseEntity.ok(updatedSupplier);
  }

  @Operation(summary = "Eliminar proveedor", description = "Elimina un proveedor que coincida con el ID proporcionado. Necesario rol ADMIN o MANAGER.")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "204", description = "Proveedor eliminado"),
    @ApiResponse(responseCode = "404", description = "Proveedor no encontrado",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteSupplier(@PathVariable int id) {
    supplierService.deleteSupplier(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Verificar existencia de NIT", description = "Verifica si existe un proveedor con el NIT proporcionado")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
  })
  @GetMapping("/exists/nit/{nit}")
  public ResponseEntity<Boolean> existsByNit(@PathVariable String nit) {
    boolean exists = supplierService.existsByNit(nit);
    return ResponseEntity.ok(exists);
  }

  @Operation(summary = "Verificar existencia de nombre", description = "Verifica si existe un proveedor con el nombre proporcionado")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
  })
  @GetMapping("/exists/name/{name}")
  public ResponseEntity<Boolean> existsByName(@PathVariable String name) {
    boolean exists = supplierService.existsByName(name);
    return ResponseEntity.ok(exists);
  }
}