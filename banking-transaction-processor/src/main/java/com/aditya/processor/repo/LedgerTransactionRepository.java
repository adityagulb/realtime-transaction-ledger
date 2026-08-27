package com.aditya.processor.repo;

import com.aditya.processor.domain.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {
}
