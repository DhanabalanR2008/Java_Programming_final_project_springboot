package com.example.expenseclaim.controller;

import com.example.expenseclaim.dto.ApprovalRequest;
import com.example.expenseclaim.dto.ClaimRequest;
import com.example.expenseclaim.dto.OverrideApprovalRequest;
import com.example.expenseclaim.dto.PaymentRequest;
import com.example.expenseclaim.model.Claim;
import com.example.expenseclaim.service.ApprovalStepService;
import com.example.expenseclaim.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class ClaimController {

    @Autowired
    private ClaimService claimService;

    @Autowired
    private ApprovalStepService approvalStepService;

    // ─── CLAIM SUBMISSION ──────────────────────────────────────────────────────

    @PostMapping("/api/claims")
    public ResponseEntity<Claim> submitClaim(@Valid @RequestBody ClaimRequest request) {
        Claim claim = claimService.submitClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(claim);
    }

    @GetMapping("/api/claims/{id}")
    public ResponseEntity<Claim> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    @GetMapping("/api/employees/{employeeId}/claims")
    public ResponseEntity<List<Claim>> getClaimsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(claimService.getClaimsByEmployee(employeeId));
    }

    @GetMapping("/api/employees/{employeeId}/claims/pending")
    public ResponseEntity<List<Claim>> getPendingClaimsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(claimService.getPendingClaimsByEmployee(employeeId));
    }

    // ─── MANAGER APPROVAL ACTIONS ──────────────────────────────────────────────

    @PutMapping("/api/claims/{claimId}/approve")
    public ResponseEntity<Claim> approveClaim(@PathVariable Long claimId,
                                              @Valid @RequestBody ApprovalRequest request) {
        Claim claim = approvalStepService.approveClaim(claimId, request);
        return ResponseEntity.ok(claim);
    }

    @PutMapping("/api/claims/{claimId}/reject")
    public ResponseEntity<Claim> rejectClaim(@PathVariable Long claimId,
                                             @Valid @RequestBody ApprovalRequest request) {
        Claim claim = approvalStepService.rejectClaim(claimId, request);
        return ResponseEntity.ok(claim);
    }

    @PutMapping("/api/claims/{claimId}/override-approve")
    public ResponseEntity<Claim> overrideApproveClaim(@PathVariable Long claimId,
                                                       @Valid @RequestBody OverrideApprovalRequest request) {
        Claim claim = approvalStepService.overrideApproveClaim(claimId, request);
        return ResponseEntity.ok(claim);
    }

    // ─── FINANCE PAYMENT ───────────────────────────────────────────────────────

    @PutMapping("/api/claims/{claimId}/pay")
    public ResponseEntity<Claim> markAsPaid(@PathVariable Long claimId,
                                            @Valid @RequestBody PaymentRequest request) {
        Claim claim = approvalStepService.markAsPaid(claimId, request);
        return ResponseEntity.ok(claim);
    }

    // ─── DELETE ENDPOINTS ──────────────────────────────────────────────────────

    @DeleteMapping("/api/claims/{claimId}")
    public ResponseEntity<java.util.Map<String, String>> deleteClaim(@PathVariable Long claimId) {
        claimService.deleteClaimById(claimId);
        return ResponseEntity.ok(java.util.Map.of("message", "Claim #" + claimId + " deleted successfully"));
    }

    @DeleteMapping("/api/claims")
    public ResponseEntity<java.util.Map<String, String>> deleteAllClaims() {
        claimService.deleteAllClaims();
        return ResponseEntity.ok(java.util.Map.of("message", "All claims deleted successfully"));
    }
}
