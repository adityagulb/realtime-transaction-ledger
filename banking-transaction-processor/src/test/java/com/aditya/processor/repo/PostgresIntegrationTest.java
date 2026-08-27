package com.aditya.processor.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    DeduplicationKeyRepository  repository;

    @Test
    void duplicateClaimIsAtomic() {

        UUID transactionId = UUID.randomUUID();

        int first = repository.claim(
                transactionId.toString(),
                Instant.now().plusSeconds(300)
        );

        int second = repository.claim(
                transactionId.toString(),
                Instant.now().plusSeconds(300)
        );

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
    }
}