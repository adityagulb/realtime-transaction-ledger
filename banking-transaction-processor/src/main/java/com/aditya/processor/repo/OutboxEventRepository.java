package com.aditya.processor.repo;

import com.aditya.processor.domain.OutboxEvent;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = "select * from outbox_event where status='NEW' order by created_at limit :limit for update skip locked", nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("limit") int limit);
}
