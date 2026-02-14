package com.hyperativa.cardapi.controller;

import com.hyperativa.cardapi.dto.BatchUploadResponse;
import com.hyperativa.cardapi.dto.CardRequest;
import com.hyperativa.cardapi.dto.CardResponse;
import com.hyperativa.cardapi.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card registration and lookup endpoints")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @Operation(summary = "Register card", description = "Registers a single card number")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardRequest request) {
        CardResponse response = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Batch upload", description = "Registers cards from a TXT file in Hyperativa format")
    public ResponseEntity<BatchUploadResponse> uploadBatch(
            @Parameter(description = "TXT file in Hyperativa format")
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        BatchUploadResponse response = cardService.uploadBatch(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search card", description = "Checks if a card exists and returns its unique identifier")
    public ResponseEntity<CardResponse> findCard(
            @Parameter(description = "Full card number")
            @RequestParam("cardNumber") String cardNumber) {

        if (cardNumber.isBlank()) {
            throw new IllegalArgumentException("The 'cardNumber' parameter is required");
        }

        return cardService.findByCardNumber(cardNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
