package com.hyperativa.cardapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_hash", columnList = "cardHash", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public unique card identifier (UUID).
     * Returned in queries instead of the internal ID.
     */
    @Column(name = "external_id", nullable = false, unique = true, updatable = false, length = 36)
    @Builder.Default
    private String externalId = UUID.randomUUID().toString();

    /**
     * Encrypted card number (AES).
     * Stored securely in the database.
     */
    @Column(name = "card_number_encrypted", nullable = false, columnDefinition = "TEXT")
    private String cardNumberEncrypted;

    /**
     * SHA-256 hash of the card number.
     * Used for efficient lookups without decryption.
     */
    @Column(name = "card_hash", nullable = false, unique = true, length = 64)
    private String cardHash;

    /**
     * Source batch (when imported via TXT file).
     */
    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
