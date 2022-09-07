package br.com.compass.filmes.cliente.dto.apiMovie;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResponseJustWatchDTO {
    private List<ResponseRentAndBuyDTO> rent;
    private List<ResponseRentAndBuyDTO> buy;
    private List<ResponseFlatrateDTO> flatrate;
}
