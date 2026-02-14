package com.hyperativa.cardapi.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        encryptionUtil = new EncryptionUtil("TestSecretKey2024!!");
    }

    @Test
    @DisplayName("Should encrypt and decrypt correctly")
    void shouldEncryptAndDecrypt() {
        String cardNumber = "4456897999999999";
        String encrypted = encryptionUtil.encrypt(cardNumber);

        assertNotNull(encrypted);
        assertNotEquals(cardNumber, encrypted);

        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals(cardNumber, decrypted);
    }

    @Test
    @DisplayName("Encryption should generate different values for same input (random IV)")
    void shouldGenerateDifferentCiphertexts() {
        String cardNumber = "4456897999999999";
        String encrypted1 = encryptionUtil.encrypt(cardNumber);
        String encrypted2 = encryptionUtil.encrypt(cardNumber);

        assertNotEquals(encrypted1, encrypted2);

        // Both should decrypt to the same value
        assertEquals(encryptionUtil.decrypt(encrypted1), encryptionUtil.decrypt(encrypted2));
    }

    @Test
    @DisplayName("Hash should be consistent for the same input")
    void shouldGenerateConsistentHash() {
        String cardNumber = "4456897999999999";
        String hash1 = encryptionUtil.hash(cardNumber);
        String hash2 = encryptionUtil.hash(cardNumber);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 = 64 hex chars
    }

    @Test
    @DisplayName("Hash should be different for different inputs")
    void shouldGenerateDifferentHashes() {
        String hash1 = encryptionUtil.hash("4456897999999999");
        String hash2 = encryptionUtil.hash("4456897922969999");

        assertNotEquals(hash1, hash2);
    }
}
