package com.example.expenseclaim.dto;

import com.example.expenseclaim.model.ExpenseItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ExpenseItemRequest {

    @NotNull(message = "Expense category must not be null")
    private ExpenseItem.ExpenseCategory category;

    @NotNull(message = "Expense amount must not be null")
    @Positive(message = "Expense amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Expense description must not be blank")
    private String description;

    @NotNull(message = "Expense date must not be null")
    private LocalDate expenseDate;
}
