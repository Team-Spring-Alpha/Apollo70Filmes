package br.com.compass.filmes.cliente.dto.apiPayment.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ResponseGatewayReproved {
    @JsonProperty("payment_status")
    private String paymentStatus = "REPROVED";
    private final String cause;

    public ResponseGatewayReproved(String cause) {
        this.cause = cause;
    }

    public ResponseGatewayReproved(String paymentStatus, String cause) {
        this.paymentStatus = paymentStatus;
        this.cause = cause;
    }
}
