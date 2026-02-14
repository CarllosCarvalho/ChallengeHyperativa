package com.hyperativa.cardapi.service;

import com.hyperativa.cardapi.dto.CardRequest;
import com.hyperativa.cardapi.dto.CardResponse;
import com.hyperativa.cardapi.entity.Card;
import com.hyperativa.cardapi.repository.CardRepository;
import com.hyperativa.cardapi.util.EncryptionUtil;
import com.hyperativa.cardapi.util.FileParserUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private FileParserUtil fileParserUtil;

    @InjectMocks
    private CardService cardService;

    @Test
    @DisplayName("Should register a new card successfully")
    void shouldCreateNewCard() {
        CardRequest request = new CardRequest();
        request.setCardNumber("4456897999999999");

        when(encryptionUtil.hash(anyString())).thenReturn("abc123hash");
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted_data");
        when(cardRepository.findByCardHash("abc123hash")).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card card = inv.getArgument(0);
            card.setId(1L);
            return card;
        });

        CardResponse response = cardService.createCard(request);

        assertNotNull(response);
        assertNotNull(response.getExternalId());
        assertEquals("Card registered successfully", response.getMessage());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("Should return existing card if already registered")
    void shouldReturnExistingCard() {
        CardRequest request = new CardRequest();
        request.setCardNumber("4456897999999999");

        Card existingCard = Card.builder()
                .id(1L)
                .externalId("uuid-123")
                .cardHash("abc123hash")
                .cardNumberEncrypted("encrypted")
                .build();

        when(encryptionUtil.hash(anyString())).thenReturn("abc123hash");
        when(cardRepository.findByCardHash("abc123hash")).thenReturn(Optional.of(existingCard));

        CardResponse response = cardService.createCard(request);

        assertNotNull(response);
        assertEquals("uuid-123", response.getExternalId());
        assertEquals("Card already registered", response.getMessage());
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find card by number")
    void shouldFindCardByNumber() {
        Card card = Card.builder()
                .id(1L)
                .externalId("uuid-456")
                .cardHash("hash123")
                .build();

        when(encryptionUtil.hash("4456897999999999")).thenReturn("hash123");
        when(cardRepository.findByCardHash("hash123")).thenReturn(Optional.of(card));

        Optional<CardResponse> result = cardService.findByCardNumber("4456897999999999");

        assertTrue(result.isPresent());
        assertEquals("uuid-456", result.get().getExternalId());
    }

    @Test
    @DisplayName("Should return empty when card not found")
    void shouldReturnEmptyWhenNotFound() {
        when(encryptionUtil.hash("0000000000000000")).thenReturn("notfoundhash");
        when(cardRepository.findByCardHash("notfoundhash")).thenReturn(Optional.empty());

        Optional<CardResponse> result = cardService.findByCardNumber("0000000000000000");

        assertFalse(result.isPresent());
    }
}
