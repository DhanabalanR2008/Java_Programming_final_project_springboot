package com.example.expenseclaim.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "claim")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnoreProperties({"claims", "hibernateLazyInitializer", "handler"})
    private Employee employee;

    @Column(nullable = false)
    private LocalDate claimDate;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    private String managerRemarks;

    private String paymentReference;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"claim", "hibernateLazyInitializer", "handler"})
    private List<ExpenseItem> expenseItems = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"claim", "hibernateLazyInitializer", "handler"})
    private List<ApprovalStep> approvalSteps = new ArrayList<>();

    public enum ClaimStatus {
        SUBMITTED,
        PENDING_APPROVAL,
        APPROVED,
        REJECTED,
        PAID
    }
}
