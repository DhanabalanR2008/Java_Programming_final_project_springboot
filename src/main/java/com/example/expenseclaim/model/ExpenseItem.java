package com.example.expenseclaim.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "expense_item")
public class ExpenseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @NotNull(message = "Expense category must not be null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @NotNull(message = "Expense amount must not be null")
    @Positive(message = "Expense amount must be greater than zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "Expense description must not be blank")
    @Column(nullable = false)
    private String description;

    @NotNull(message = "Expense date must not be null")
    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal policyLimit;

    @Column(nullable = false)
    private boolean exceedsPolicyLimit = false;

    /**
     * Supported expense categories with their respective policy limits (in INR).
     */
    public enum ExpenseCategory {
        TRAVEL(new BigDecimal("5000")),
        FOOD(new BigDecimal("1000")),
        HOTEL(new BigDecimal("5000")),
        FUEL(new BigDecimal("3000"));

        private final BigDecimal policyLimit;

        ExpenseCategory(BigDecimal policyLimit) {
            this.policyLimit = policyLimit;
        }

        public BigDecimal getPolicyLimit() {
            return policyLimit;
        }
    }
}
