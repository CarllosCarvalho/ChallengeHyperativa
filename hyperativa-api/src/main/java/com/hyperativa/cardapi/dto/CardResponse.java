package com.hyperativa.cardapi.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponse {
    private String externalId;
    private String message;
}
