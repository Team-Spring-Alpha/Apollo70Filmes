package br.com.compass.filmes.cliente.proxy;

import br.com.compass.filmes.cliente.dto.apiPayment.request.RequestAuth;
import br.com.compass.filmes.cliente.dto.apiPayment.request.RequestPayment;
import br.com.compass.filmes.cliente.enums.ClientEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = GatewayProxy.class)
class GatewayProxyTest {
    @Autowired
    private GatewayProxy gatewayProxy;

    @MockBean
    private Gateway gateway;

    @Test
    @DisplayName("should get auth token sucessful")
    void shouldGetAuthTokenSucessful() {
        RequestAuth requestAuth = new RequestAuth(ClientEnum.PEDRO.getClientId(), ClientEnum.PEDRO.getApiKey());

        gatewayProxy.getAuthToken(ClientEnum.PEDRO);

        Mockito.verify(gateway).getAuthToken(requestAuth);
    }

    @Test
    @DisplayName("should get payment from gateway")
    void shouldGetPaymentFromGateWay() {
        RequestPayment requestPayment = new RequestPayment();
        gatewayProxy.getPayment("test", requestPayment);

        Mockito.verify(gateway).getPayment("Bearer test", requestPayment);
    }
}