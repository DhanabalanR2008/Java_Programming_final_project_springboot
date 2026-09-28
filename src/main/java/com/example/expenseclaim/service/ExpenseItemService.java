package com.example.expenseclaim.service;

import com.example.expenseclaim.exception.ResourceNotFoundException;
import com.example.expenseclaim.model.ExpenseItem;
import com.example.expenseclaim.repository.ClaimRepository;
import com.example.expenseclaim.repository.ExpenseItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseItemService {

    @Autowired
    private ExpenseItemRepository expenseItemRepository;

    @Autowired
    private ClaimRepository claimRepository;

    public List<ExpenseItem> getExpenseItemsForClaim(Long claimId) {
        // Verify claim exists
        claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));
        return expenseItemRepository.findByClaimId(claimId);
    }

    public ExpenseItem getExpenseItemById(Long id) {
        return expenseItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense item not found with id: " + id));
    }
}
