package com.example.expenseclaim.service;

import com.example.expenseclaim.dto.ClaimRequest;
import com.example.expenseclaim.dto.ExpenseItemRequest;
import com.example.expenseclaim.exception.ResourceNotFoundException;
import com.example.expenseclaim.model.ApprovalStep;
import com.example.expenseclaim.model.Claim;
import com.example.expenseclaim.model.Employee;
import com.example.expenseclaim.model.ExpenseItem;
import com.example.expenseclaim.repository.ApprovalStepRepository;
import com.example.expenseclaim.repository.ClaimRepository;
import com.example.expenseclaim.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClaimService {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ApprovalStepRepository approvalStepRepository;

    /**
     * Submits a new expense claim for an employee.
     * Steps:
     *  1. Verify employee exists
     *  2. Validate and build expense items
     *  3. Auto-calculate total
     *  4. Flag items exceeding category policy limits
     *  5. Save claim and items
     *  6. Create an initial PENDING ApprovalStep
     *  7. Set claim status to PENDING_APPROVAL
     */
    @Transactional
    public Claim submitClaim(ClaimRequest request) {
        // 1. Verify employee exists
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + request.getEmployeeId()));

        // 2. Build the Claim entity
        Claim claim = new Claim();
        claim.setEmployee(employee);
        claim.setClaimDate(request.getClaimDate());
        claim.setDescription(request.getDescription());
        claim.setStatus(Claim.ClaimStatus.SUBMITTED);

        // 3. Build expense items, calculate total, flag policy violations
        List<ExpenseItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ExpenseItemRequest itemRequest : request.getExpenseItems()) {
            ExpenseItem item = new ExpenseItem();
            item.setClaim(claim);
            item.setCategory(itemRequest.getCategory());
            item.setAmount(itemRequest.getAmount());
            item.setDescription(itemRequest.getDescription());
            item.setExpenseDate(itemRequest.getExpenseDate());

            // Retrieve policy limit from category enum
            BigDecimal policyLimit = itemRequest.getCategory().getPolicyLimit();
            item.setPolicyLimit(policyLimit);

            // Flag if amount exceeds category policy limit
            boolean exceeds = itemRequest.getAmount().compareTo(policyLimit) > 0;
            item.setExceedsPolicyLimit(exceeds);

            totalAmount = totalAmount.add(itemRequest.getAmount());
            items.add(item);
        }

        // 4. Set total amount (backend-calculated, not trusted from client)
        claim.setTotalAmount(totalAmount);
        claim.setExpenseItems(items);

        // 5. Move to PENDING_APPROVAL
        claim.setStatus(Claim.ClaimStatus.PENDING_APPROVAL);

        // 6. Save the claim (cascade saves expense items)
        Claim savedClaim = claimRepository.save(claim);

        // 7. Create initial ApprovalStep
        ApprovalStep approvalStep = new ApprovalStep();
        approvalStep.setClaim(savedClaim);
        approvalStep.setApproverName(employee.getManagerName());
        approvalStep.setStatus(ApprovalStep.ApprovalStatus.PENDING);
        approvalStepRepository.save(approvalStep);

        return savedClaim;
    }

    public Claim getClaimById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
    }

    public List<Claim> getClaimsByEmployee(Long employeeId) {
        // Verify employee exists
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        return claimRepository.findByEmployeeId(employeeId);
    }

    public List<Claim> getPendingClaimsByEmployee(Long employeeId) {
        // Verify employee exists
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        return claimRepository.findByEmployeeIdAndStatus(employeeId, Claim.ClaimStatus.PENDING_APPROVAL);
    }

    @Transactional
    public void deleteClaimById(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
        claimRepository.delete(claim);
    }

    @Transactional
    public void deleteAllClaims() {
        claimRepository.deleteAll();
    }
}
