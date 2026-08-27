package com.aditya.processor.repo;

import com.aditya.processor.domain.RejectedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RejectedTransactionRepository extends JpaRepository<RejectedTransaction, UUID> {
}
