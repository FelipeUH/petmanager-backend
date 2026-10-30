package com.codefactory.petmanager.g12.petmanager_backend.payment.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.codefactory.petmanager.g12.petmanager_backend.supplier.model.Supplier;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "supplier_id", nullable = false)
  private Supplier supplier;

  @OneToMany(mappedBy = "payment", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<PaymentsProducts> paymentsProducts;

  @Column(name = "payment_date")
  private LocalDate paymentDate;

  @Column(precision = 38, scale = 2, columnDefinition = "NUMERIC(38,2)", nullable = false)
  private BigDecimal amount;

  @Column(columnDefinition = "TEXT")
  private String notes;

}
