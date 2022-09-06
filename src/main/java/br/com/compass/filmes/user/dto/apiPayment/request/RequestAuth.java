package br.com.compass.filmes.user.dto.apiPayment.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class RequestAuth {
    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("api_key")
    private String apiKey;
}