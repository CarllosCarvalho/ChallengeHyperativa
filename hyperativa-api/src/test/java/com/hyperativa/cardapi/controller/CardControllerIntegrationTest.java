package com.hyperativa.cardapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperativa.cardapi.dto.AuthRequest;
import com.hyperativa.cardapi.dto.AuthResponse;
import com.hyperativa.cardapi.dto.CardRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String getToken() throws Exception {
        AuthRequest authRequest = new AuthRequest("admin", "admin123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return authResponse.getToken();
    }

    @Test
    @Order(1)
    @DisplayName("Should reject access without token")
    void shouldRejectWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/cards/search")
                        .param("cardNumber", "4456897999999999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(2)
    @DisplayName("Should register card successfully")
    void shouldCreateCard() throws Exception {
        String token = getToken();
        CardRequest request = new CardRequest();
        request.setCardNumber("4456897999999999");

        mockMvc.perform(post("/api/v1/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalId").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Card registered successfully"));
    }

    @Test
    @Order(3)
    @DisplayName("Should find existing card")
    void shouldFindExistingCard() throws Exception {
        String token = getToken();

        // Register first
        CardRequest request = new CardRequest();
        request.setCardNumber("4456897922969999");

        mockMvc.perform(post("/api/v1/cards")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Search
        mockMvc.perform(get("/api/v1/cards/search")
                        .header("Authorization", "Bearer " + token)
                        .param("cardNumber", "4456897922969999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").isNotEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 for non-existent card")
    void shouldReturn404ForNonExistentCard() throws Exception {
        String token = getToken();

        mockMvc.perform(get("/api/v1/cards/search")
                        .header("Authorization", "Bearer " + token)
                        .param("cardNumber", "0000000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("Should process batch file upload")
    void shouldProcessBatchUpload() throws Exception {
        String token = getToken();

        String fileContent = """
                DESAFIO-HYPERATIVA           20180524LOTE0001000003
                C1     4456897912349999
                C2     4456897956789999
                C3     4456897943219999
                LOTE0001000003
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "cards.txt", "text/plain", fileContent.getBytes());

        mockMvc.perform(multipart("/api/v1/cards/batch")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(3))
                .andExpect(jsonPath("$.totalSuccess").value(3));
    }

    @Test
    @Order(6)
    @DisplayName("Should reject card with invalid format")
    void shouldRejectInvalidCardFormat() throws Exception {
        String token = getToken();
        CardRequest request = new CardRequest();
        request.setCardNumber("123"); // too short

        mockMvc.perform(post("/api/v1/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
