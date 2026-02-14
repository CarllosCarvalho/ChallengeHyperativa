package com.hyperativa.cardapi.service;

import com.hyperativa.cardapi.dto.BatchUploadResponse;
import com.hyperativa.cardapi.dto.CardRequest;
import com.hyperativa.cardapi.dto.CardResponse;
import com.hyperativa.cardapi.entity.Card;
import com.hyperativa.cardapi.repository.CardRepository;
import com.hyperativa.cardapi.util.EncryptionUtil;
import com.hyperativa.cardapi.util.FileParserUtil;
import com.hyperativa.cardapi.util.FileParserUtil.ParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final EncryptionUtil encryptionUtil;
    private final FileParserUtil fileParserUtil;

    /**
     * Registers a single card.
     */
    @Transactional
    public CardResponse createCard(CardRequest request) {
        String cardNumber = request.getCardNumber().trim();
        String hash = encryptionUtil.hash(cardNumber);

        // Check if already exists
        Optional<Card> existing = cardRepository.findByCardHash(hash);
        if (existing.isPresent()) {
            log.info("Card already registered, returning existing identifier");
            return CardResponse.builder()
                    .externalId(existing.get().getExternalId())
                    .message("Card already registered")
                    .build();
        }

        Card card = Card.builder()
                .cardNumberEncrypted(encryptionUtil.encrypt(cardNumber))
                .cardHash(hash)
                .build();

        card = cardRepository.save(card);
        log.info("Card registered successfully. ExternalId: {}", card.getExternalId());

        return CardResponse.builder()
                .externalId(card.getExternalId())
                .message("Card registered successfully")
                .build();
    }

    /**
     * Processes a TXT file in batch.
     */
    @Transactional
    public BatchUploadResponse uploadBatch(MultipartFile file) throws IOException {
        ParseResult parseResult = fileParserUtil.parse(file.getInputStream());
        List<String> errors = new ArrayList<>(parseResult.getErrors());
        int successCount = 0;

        for (String cardNumber : parseResult.getCardNumbers()) {
            try {
                String hash = encryptionUtil.hash(cardNumber);

                if (cardRepository.existsByCardHash(hash)) {
                    log.debug("Card already exists in batch, skipping duplicate");
                    successCount++;
                    continue;
                }

                Card card = Card.builder()
                        .cardNumberEncrypted(encryptionUtil.encrypt(cardNumber))
                        .cardHash(hash)
                        .batchId(parseResult.getBatchId())
                        .build();

                cardRepository.save(card);
                successCount++;
            } catch (Exception e) {
                errors.add("Error processing card: " + e.getMessage());
                log.error("Error processing card in batch", e);
            }
        }

        log.info("Batch '{}' processed: {}/{} cards successful",
                parseResult.getBatchId(), successCount, parseResult.getCardNumbers().size());

        return BatchUploadResponse.builder()
                .batchId(parseResult.getBatchId())
                .totalProcessed(parseResult.getCardNumbers().size())
                .totalSuccess(successCount)
                .totalErrors(errors.size())
                .errors(errors)
                .build();
    }

    /**
     * Looks up a card by number and returns its unique identifier.
     */
    public Optional<CardResponse> findByCardNumber(String cardNumber) {
        String hash = encryptionUtil.hash(cardNumber.trim());
        return cardRepository.findByCardHash(hash)
                .map(card -> CardResponse.builder()
                        .externalId(card.getExternalId())
                        .message("Card found")
                        .build());
    }
}
