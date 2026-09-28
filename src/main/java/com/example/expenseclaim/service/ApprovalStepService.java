package com.example.expenseclaim.service;

import com.example.expenseclaim.dto.ApprovalRequest;
import com.example.expenseclaim.dto.OverrideApprovalRequest;
import com.example.expenseclaim.dto.PaymentRequest;
import com.example.expenseclaim.exception.BusinessRuleException;
import com.example.expenseclaim.exception.InvalidStateException;
import com.example.expenseclaim.exception.ResourceNotFoundException;
import com.example.expenseclaim.model.ApprovalStep;
import com.example.expenseclaim.model.Claim;
import com.example.expenseclaim.model.ExpenseItem;
import com.example.expenseclaim.repository.ApprovalStepRepository;
import com.example.expenseclaim.repository.ClaimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalStepService {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ApprovalStepRepository approvalStepRepository;

    /**
     * Manager approves a claim using the standard approval flow.
     *
     * Business Rule 1 (Policy Limit Override):
     *   If any expense item in the claim exceeds its category policy limit,
     *   the system rejects normal approval and requires manager to use override-approve instead.
     */
    @Transactional
    public Claim approveClaim(Long claimId, ApprovalRequest request) {
        Claim claim = getClaimInPendingState(claimId);

        // Business Rule 1: Check for policy violations — reject normal approval if any item exceeds limit
        boolean hasPolicyViolation = claim.getExpenseItems().stream()
                .anyMatch(ExpenseItem::isExceedsPolicyLimit);

        if (hasPolicyViolation) {
            throw new BusinessRuleException(
                    "Claim requires manager override because one or more expense items exceed the policy limit. " +
                    "Please use the override-approve endpoint instead.");
        }

        // Update ApprovalStep
        ApprovalStep approvalStep = getLatestApprovalStep(claimId);
        approvalStep.setApproverName(request.getApproverName());
        approvalStep.setStatus(ApprovalStep.ApprovalStatus.APPROVED);
        approvalStep.setRemarks(request.getRemarks());
        approvalStep.setApprovedAt(LocalDateTime.now());
        approvalStepRepository.save(approvalStep);

        // Update Claim status
        claim.setManagerRemarks(request.getRemarks());
        claim.setStatus(Claim.ClaimStatus.APPROVED);
        return claimRepository.save(claim);
    }

    /**
     * Manager rejects a claim.
     */
    @Transactional
    public Claim rejectClaim(Long claimId, ApprovalRequest request) {
        Claim claim = getClaimInPendingState(claimId);

        // Update ApprovalStep
        ApprovalStep approvalStep = getLatestApprovalStep(claimId);
        approvalStep.setApproverName(request.getApproverName());
        approvalStep.setStatus(ApprovalStep.ApprovalStatus.REJECTED);
        approvalStep.setRemarks(request.getRemarks());
        approvalStep.setApprovedAt(LocalDateTime.now());
        approvalStepRepository.save(approvalStep);

        // Update Claim status
        claim.setManagerRemarks(request.getRemarks());
        claim.setStatus(Claim.ClaimStatus.REJECTED);
        return claimRepository.save(claim);
    }

    /**
     * Manager override-approves a claim that contains expense items exceeding policy limits.
     * Remarks are MANDATORY for override approval.
     *
     * Business Rule 1 (Override path):
     *   This endpoint is ONLY for claims with policy violations.
     *   It explicitly records that the manager acknowledged and overrode the policy limits.
     */
    @Transactional
    public Claim overrideApproveClaim(Long claimId, OverrideApprovalRequest request) {
        Claim claim = getClaimInPendingState(claimId);

        boolean hasPolicyViolation = claim.getExpenseItems().stream()
                .anyMatch(ExpenseItem::isExceedsPolicyLimit);

        if (!hasPolicyViolation) {
            throw new BusinessRuleException(
                    "Override approval is not required. This claim has no policy violations. " +
                    "Please use the regular approval endpoint.");
        }

        // Update ApprovalStep with OVERRIDE_APPROVED
        ApprovalStep approvalStep = getLatestApprovalStep(claimId);
        approvalStep.setApproverName(request.getApproverName());
        approvalStep.setStatus(ApprovalStep.ApprovalStatus.OVERRIDE_APPROVED);
        approvalStep.setRemarks(request.getRemarks());
        approvalStep.setApprovedAt(LocalDateTime.now());
        approvalStepRepository.save(approvalStep);

        // Update Claim status — override still results in APPROVED
        claim.setManagerRemarks(request.getRemarks());
        claim.setStatus(Claim.ClaimStatus.APPROVED);
        return claimRepository.save(claim);
    }

    /**
     * Finance marks an approved claim as PAID.
     *
     * Business Rule 2 (Finance Payment):
     *   A claim can only be marked as PAID after it has been manager-approved (status = APPROVED).
     *   Attempting to pay a rejected, submitted, or already-paid claim results in a clear error.
     */
    @Transactional
    public Claim markAsPaid(Long claimId, PaymentRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        // Business Rule 2: Only APPROVED claims can be paid
        switch (claim.getStatus()) {
            case PAID ->
                throw new InvalidStateException("Claim has already been paid.");
            case REJECTED ->
                throw new InvalidStateException("Claim has already been rejected. It cannot be marked as paid.");
            case SUBMITTED, PENDING_APPROVAL ->
                throw new BusinessRuleException(
                        "Claim cannot be marked as paid because manager approval is required.");
            case APPROVED -> {
                // Valid — proceed to mark as paid
                claim.setPaymentReference(request.getPaymentReference());
                claim.setStatus(Claim.ClaimStatus.PAID);
            }
        }

        return claimRepository.save(claim);
    }

    // ─── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private Claim getClaimInPendingState(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        if (claim.getStatus() == Claim.ClaimStatus.APPROVED) {
            throw new InvalidStateException("Claim has already been approved.");
        }
        if (claim.getStatus() == Claim.ClaimStatus.REJECTED) {
            throw new InvalidStateException("Claim has already been rejected.");
        }
        if (claim.getStatus() == Claim.ClaimStatus.PAID) {
            throw new InvalidStateException("Claim has already been paid. No further approval actions are allowed.");
        }
        if (claim.getStatus() == Claim.ClaimStatus.SUBMITTED) {
            throw new BusinessRuleException("Claim is still in SUBMITTED state and cannot be actioned yet.");
        }

        return claim;
    }

    private ApprovalStep getLatestApprovalStep(Long claimId) {
        return approvalStepRepository.findTopByClaimIdOrderByIdDesc(claimId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No approval step found for claim id: " + claimId));
    }

    public List<ApprovalStep> getApprovalStepsForClaim(Long claimId) {
        // Verify claim exists
        claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));
        return approvalStepRepository.findByClaimId(claimId);
    }
}
