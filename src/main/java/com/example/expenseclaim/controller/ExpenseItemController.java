package com.example.expenseclaim.controller;

import com.example.expenseclaim.model.ApprovalStep;
import com.example.expenseclaim.model.ExpenseItem;
import com.example.expenseclaim.service.ApprovalStepService;
import com.example.expenseclaim.service.ExpenseItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@CrossOrigin(origins = "*")
public class ExpenseItemController {

    @Autowired
    private ExpenseItemService expenseItemService;

    @Autowired
    private ApprovalStepService approvalStepService;

    @GetMapping("/{claimId}/items")
    public ResponseEntity<List<ExpenseItem>> getExpenseItems(@PathVariable Long claimId) {
        return ResponseEntity.ok(expenseItemService.getExpenseItemsForClaim(claimId));
    }

    @GetMapping("/{claimId}/approval-steps")
    public ResponseEntity<List<ApprovalStep>> getApprovalSteps(@PathVariable Long claimId) {
        return ResponseEntity.ok(approvalStepService.getApprovalStepsForClaim(claimId));
    }
}
