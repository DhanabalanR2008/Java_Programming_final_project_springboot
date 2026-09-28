package com.example.expenseclaim.repository;

import com.example.expenseclaim.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByEmployeeId(Long employeeId);

    List<Claim> findByEmployeeIdAndStatus(Long employeeId, Claim.ClaimStatus status);
}
