package com.aditya.processor.outbox;

import com.aditya.processor.repo.DeduplicationKeyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DedupCleanupJob {
    private final DeduplicationKeyRepository repo;

    public DedupCleanupJob(DeduplicationKeyRepository repo) {
        this.repo = repo;
    }

    @Scheduled(fixedDelayString = "${app.dedup-cleanup-ms:60000}")
    public void cleanup() {
        repo.deleteExpired(Instant.now());
    }
}
