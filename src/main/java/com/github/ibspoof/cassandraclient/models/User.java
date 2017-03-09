package com.github.ibspoof.cassandraclient.models;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(name = "users",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false)
public class User {
    @PartitionKey
    @Column(name = "user_id")
    private UUID userId;
    private String name;
    // ... constructors / getters / setters
}