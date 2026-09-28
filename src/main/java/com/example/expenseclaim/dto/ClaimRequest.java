package com.example.expenseclaim.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class ClaimRequest {

    @NotNull(message = "Employee ID must not be null")
    private Long employeeId;

    @NotNull(message = "Claim date must not be null")
    private LocalDate claimDate;

    @NotBlank(message = "Claim description must not be blank")
    private String description;

    @NotEmpty(message = "Claim must contain at least one expense item")
    @Valid
    private List<ExpenseItemRequest> expenseItems;
}
