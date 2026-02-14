package com.hyperativa.cardapi.util;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for TXT files in Hyperativa format.
 * <p>
 * Header format (line 1):
 * - [01-29] NAME
 * - [30-37] DATE (YYYYMMDD)
 * - [38-45] BATCH
 * - [46-51] RECORD COUNT
 * <p>
 * Card line format:
 * - [01-01] LINE IDENTIFIER (C)
 * - [02-07] BATCH NUMBER
 * - [08-26] FULL CARD NUMBER
 * <p>
 * Footer format (last line):
 * - [01-08] BATCH
 * - [09-14] RECORD COUNT
 */
@Component
public class FileParserUtil {

    @Getter
    @Builder
    public static class ParseResult {
        private String batchId;
        private String batchDate;
        private String batchName;
        private int expectedCount;
        private List<String> cardNumbers;
        private List<String> errors;
    }

    /**
     * Parses the TXT file and extracts card numbers.
     */
    public ParseResult parse(InputStream inputStream) {
        List<String> cardNumbers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String batchId = null;
        String batchDate = null;
        String batchName = null;
        int expectedCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines()
                    .filter(line -> !line.isBlank())
                    .toList();

            if (lines.isEmpty()) {
                errors.add("Empty file");
                return buildResult(batchId, batchDate, batchName, expectedCount, cardNumbers, errors);
            }

            // Parse header (first line)
            String header = padRight(lines.getFirst(), 51);
            batchName = header.substring(0, Math.min(29, header.length())).trim();
            if (header.length() >= 37) {
                batchDate = header.substring(29, 37).trim();
            }
            if (header.length() >= 45) {
                batchId = header.substring(37, 45).trim();
            }
            if (header.length() >= 51) {
                String countStr = header.substring(45, 51).trim();
                try {
                    expectedCount = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    errors.add("Invalid record count in header: " + countStr);
                }
            }

            // Parse card lines (skip header and footer)
            for (int i = 1; i < lines.size() - 1; i++) {
                String line = lines.get(i);

                // Check if the line starts with "C" (card identifier)
                if (!line.isEmpty() && line.charAt(0) == 'C') {
                    if (line.length() <= 7) {
                        errors.add("Line " + (i + 1) + ": invalid format");
                        continue;
                    }

                    // Extract all content after position 7 to validate actual size
                    String cardPart = line.substring(7).trim();
                    String cardNumber = cardPart.replaceAll("\\s+", "");

                    // Validate that it is numeric and has between 13 and 19 digits
                    if (cardNumber.matches("\\d{13,19}")) {
                        cardNumbers.add(cardNumber);
                    } else {
                        errors.add("Line " + (i + 1) + ": invalid card number '" + cardNumber + "'");
                    }
                }
            }

        } catch (Exception e) {
            errors.add("Error processing file: " + e.getMessage());
        }

        return buildResult(batchId, batchDate, batchName, expectedCount, cardNumbers, errors);
    }

    private ParseResult buildResult(String batchId, String batchDate, String batchName,
                                     int expectedCount, List<String> cardNumbers, List<String> errors) {
        return ParseResult.builder()
                .batchId(batchId)
                .batchDate(batchDate)
                .batchName(batchName)
                .expectedCount(expectedCount)
                .cardNumbers(cardNumbers)
                .errors(errors)
                .build();
    }

    private String padRight(String str, int length) {
        if (str.length() >= length) return str;
        return str + " ".repeat(length - str.length());
    }
}
