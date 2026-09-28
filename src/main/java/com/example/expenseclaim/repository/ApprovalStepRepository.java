package com.example.expenseclaim.repository;

import com.example.expenseclaim.model.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    List<ApprovalStep> findByClaimId(Long claimId);

    Optional<ApprovalStep> findTopByClaimIdOrderByIdDesc(Long claimId);
}
