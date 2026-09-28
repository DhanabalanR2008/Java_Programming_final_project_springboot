package com.example.expenseclaim.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Payment reference must not be blank")
    private String paymentReference;
}
