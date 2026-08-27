package com.aditya.processor.repo;

import com.aditya.processor.domain.DeduplicationKey;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface DeduplicationKeyRepository extends JpaRepository<DeduplicationKey, String> {
    @Modifying
    @Transactional
    @Query(value = "insert into deduplication_key(transaction_id,expires_at) values (:id,:expires) on conflict (transaction_id) do update set expires_at=excluded.expires_at where deduplication_key.expires_at < now()", nativeQuery = true)
    int claim(@Param("id") String id, @Param("expires") Instant expires);

    @Modifying
    @Transactional
    @Query(value = "delete from deduplication_key where expires_at < :now", nativeQuery = true)
    int deleteExpired(@Param("now") Instant now);
}
