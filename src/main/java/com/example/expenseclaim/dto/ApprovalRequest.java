package com.example.expenseclaim.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApprovalRequest {

    @NotBlank(message = "Approver name must not be blank")
    private String approverName;

    private String remarks;
}
