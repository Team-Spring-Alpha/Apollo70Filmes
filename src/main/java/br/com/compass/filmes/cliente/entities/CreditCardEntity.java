package br.com.compass.filmes.cliente.entities;

import br.com.compass.filmes.cliente.enums.ClientCreditCardBrandEnum;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "CreditCard")
public class CreditCardEntity {
    private ClientCreditCardBrandEnum brand;
    private String number;
    private String securityCode;
    private String yearExpiration;
    private String monthExpiration;
    private String holderName;
}
