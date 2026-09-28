package com.example.expenseclaim.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OverrideApprovalRequest {

    @NotBlank(message = "Approver name must not be blank")
    private String approverName;

    @NotBlank(message = "Override remarks are required when approving claims with policy violations")
    private String remarks;
}
