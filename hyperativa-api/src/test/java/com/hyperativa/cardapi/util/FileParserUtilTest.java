package com.hyperativa.cardapi.util;

import com.hyperativa.cardapi.util.FileParserUtil.ParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FileParserUtilTest {

    private FileParserUtil fileParserUtil;

    @BeforeEach
    void setUp() {
        fileParserUtil = new FileParserUtil();
    }

    @Test
    @DisplayName("Should parse file in Hyperativa format")
    void shouldParseHyperativaFile() {
        String content = """
                DESAFIO-HYPERATIVA           20180524LOTE0001000010
                C1     4456897922969999
                C2     4456897999999999
                C3     4456897998199999
                LOTE0001000010
                """;

        ParseResult result = fileParserUtil.parse(toStream(content));

        assertEquals("LOTE0001", result.getBatchId());
        assertEquals("20180524", result.getBatchDate());
        assertEquals(3, result.getCardNumbers().size());
        assertTrue(result.getCardNumbers().contains("4456897922969999"));
        assertTrue(result.getCardNumbers().contains("4456897999999999"));
        assertTrue(result.getCardNumbers().contains("4456897998199999"));
    }

    @Test
    @DisplayName("Should reject cards with invalid format")
    void shouldRejectInvalidCards() {
        String content = """
                DESAFIO-HYPERATIVA           20180524LOTE0001000010
                C1     44568979999999812123123123123
                C2     4456897999999999124
                C3     4456897922969999123123123123123
                LOTE0001000010
                """;

        ParseResult result = fileParserUtil.parse(toStream(content));

        // Only valid cards (13-19 digits) should be accepted
        assertEquals(1, result.getCardNumbers().size());
        assertTrue(result.getCardNumbers().contains("4456897999999999124"));
        assertTrue(result.getErrors().size() >= 1);
    }

    @Test
    @DisplayName("Should handle empty file")
    void shouldHandleEmptyFile() {
        ParseResult result = fileParserUtil.parse(toStream(""));
        assertTrue(result.getCardNumbers().isEmpty());
        assertFalse(result.getErrors().isEmpty());
    }

    private ByteArrayInputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
