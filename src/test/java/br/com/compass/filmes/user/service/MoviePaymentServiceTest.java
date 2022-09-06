package br.com.compass.filmes.user.service;

import br.com.compass.filmes.user.builders.*;
import br.com.compass.filmes.user.dto.movie.manager.RequestMoviePayment;
import br.com.compass.filmes.user.dto.movie.manager.RequestRentOrBuy;
import br.com.compass.filmes.user.dto.payment.response.ResponseAuth;
import br.com.compass.filmes.user.dto.payment.response.ResponseGatewayReproved;
import br.com.compass.filmes.user.dto.payment.response.ResponsePayment;
import br.com.compass.filmes.user.dto.payment.response.ResponseProcessPayment;
import br.com.compass.filmes.user.dto.user.response.apiMovie.ResponseJustWatch;
import br.com.compass.filmes.user.dto.user.response.apiMovie.ResponseMovieById;
import br.com.compass.filmes.user.dto.user.response.apiMovie.ResponseRentAndBuy;
import br.com.compass.filmes.user.entities.UserEntity;
import br.com.compass.filmes.user.entities.CreditCardEntity;
import br.com.compass.filmes.user.enums.MovieLinks;
import br.com.compass.filmes.user.exceptions.BuyMovieNotFoundException;
import br.com.compass.filmes.user.exceptions.UserNotFoundException;
import br.com.compass.filmes.user.exceptions.CreditCardNotFoundException;
import br.com.compass.filmes.user.exceptions.RentMovieNotFoundException;
import br.com.compass.filmes.user.client.GatewayProxy;
import br.com.compass.filmes.user.client.MovieSearchProxy;
import br.com.compass.filmes.user.rabbitMq.MessageHistory;
import br.com.compass.filmes.user.repository.UserRepository;
import br.com.compass.filmes.user.util.Md5;
import br.com.compass.filmes.user.util.ValidRequestMoviePayment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(classes = MoviePaymentService.class)
class MoviePaymentServiceTest {

    @Autowired
    private MoviePaymentService moviePaymentService;

    @SpyBean
    private ModelMapper modelMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MovieSearchProxy movieSearchProxy;

    @MockBean
    private  GatewayProxy gatewayProxy;

    @MockBean
    private  MessageHistory messageHistory;

    @MockBean
    private  Md5 md5;

    @MockBean
    private ValidRequestMoviePayment validRequestMoviePayment;

    @Test
    @DisplayName("should throw client not found exception when not found a client by id")
    void shoudThrowClientNotFoundWhenNotFoundAClient() {
        UserEntity userEntity = UserEntityBuilder.one().withId("1L").now();
        RequestMoviePayment requestMoviePayment = new RequestMoviePayment();
        requestMoviePayment.setUserId("2L");

        Assertions.assertThrows(UserNotFoundException.class, () -> moviePaymentService.post(requestMoviePayment));
    }

    @Test
    @DisplayName("should throw credit card not found when not found a credt card from that client")
    void shoudThrowCreditCardNotFoundWhenNotFoundACreditCard() {
        RequestMoviePayment requestMoviePayment = new RequestMoviePayment();
        requestMoviePayment.setCreditCardNumber("not found");
        UserEntity userEntity = UserEntityBuilder.one().now();

        Mockito.when(userRepository.findById(any())).thenReturn(Optional.ofNullable(userEntity));

        Assertions.assertThrows(CreditCardNotFoundException.class, () -> moviePaymentService.post(requestMoviePayment));
    }


    @Test
    @DisplayName("should throw buy movie not found when external api dont return where to buy that movie")
    void shouldThrowBuyMovieNotFoundExceptionWhenExternalApiReturnNullFromBuyProviderList() {
        RequestRentOrBuy rentOrBuy = RequestRentOrBuyBuilder.one().withRentList(null).now();

        RequestMoviePayment moviePayment = RequestMoviePaymentBuilder.one()
                .withCreditCardNumber("test")
                .withRentOrBuy(rentOrBuy)
                .now();

        UserEntity userEntity = buildClientEntityWithCreditCardNumber("test");

        ResponseMovieById responseMovieById = buildResponseMovieById();
        responseMovieById.getJustWatch().setBuy(null);

        Mockito.when(userRepository.findById(any())).thenReturn(Optional.of(userEntity));
        Mockito.when(movieSearchProxy.getMovieById(any())).thenReturn(responseMovieById);

        Assertions.assertThrows(BuyMovieNotFoundException.class, () -> moviePaymentService.post(moviePayment));
    }

    @Test
    @DisplayName("should throw rent movie not found when external api dont return where to rent that movie")
    void shouldThrowBuyMovieNotFoundExceptionWhenExternalApiReturnNullFromRentProviderList() {
        RequestRentOrBuy rentOrBuy = RequestRentOrBuyBuilder.one().withBuyList(null).now();
        RequestMoviePayment moviePayment = RequestMoviePaymentBuilder.one()
                .withCreditCardNumber("test")
                .withRentOrBuy(rentOrBuy)
                .now();

        UserEntity userEntity = buildClientEntityWithCreditCardNumber("test");

        ResponseMovieById responseMovieById = buildResponseMovieById();
        responseMovieById.getJustWatch().setRent(null);

        Mockito.when(userRepository.findById(any())).thenReturn(Optional.of(userEntity));
        Mockito.when(movieSearchProxy.getMovieById(any())).thenReturn(responseMovieById);

        Assertions.assertThrows(RentMovieNotFoundException.class, () -> moviePaymentService.post(moviePayment));
    }

    @Test
    @DisplayName("should process with sucessful a payment request reproved by gateway")
    void shouldProcessWithSucessfulAPaymentReprovedByGateway() {
        RequestMoviePayment moviePayment = RequestMoviePaymentBuilder.one().withCreditCardNumber("test").now();
        UserEntity userEntity = buildClientEntityWithCreditCardNumber("test");

        ResponseMovieById responseMovieById = buildResponseMovieById();

        ResponseAuth responseAuth = ResponseAuthBuilder.one().now();
        ResponsePayment responsePayment = buildResponsePaymentFailed();

        Mockito.when(userRepository.findById(any())).thenReturn(Optional.of(userEntity));
        Mockito.when(movieSearchProxy.getMovieById(any())).thenReturn(responseMovieById);
        Mockito.when(gatewayProxy.getAuthToken(any())).thenReturn(responseAuth);
        Mockito.when(gatewayProxy.getPayment(any(), any())).thenReturn(responsePayment);

        ResponseGatewayReproved responseGatewayReproved = moviePaymentService.post(moviePayment);

        Assertions.assertEquals(responsePayment.getStatus(), responseGatewayReproved.getPaymentStatus());
        Assertions.assertEquals(responsePayment.getAuthorization().getReasonMessage(), responseGatewayReproved.getCause());
    }

    @Test
    @DisplayName("should process with sucessful a payment request approved by gateway")
    void shouldProcessWithSucessfulAPaymentApprovedByGateway() {
        RequestMoviePayment moviePayment = RequestMoviePaymentBuilder.one().withCreditCardNumber("0081").now();
        UserEntity userEntity = buildClientEntityWithCreditCardNumber("0081");

        ResponseMovieById responseMovieById = buildResponseMovieById();

        ResponseAuth responseAuth = ResponseAuthBuilder.one().now();
        ResponsePayment responsePayment = buildResponsePaymentApproved();

        Mockito.when(userRepository.findById(any())).thenReturn(Optional.of(userEntity));
        Mockito.when(movieSearchProxy.getMovieById(any())).thenReturn(responseMovieById);
        Mockito.when(gatewayProxy.getAuthToken(any())).thenReturn(responseAuth);
        Mockito.when(gatewayProxy.getPayment(any(), any())).thenReturn(responsePayment);

        ResponseGatewayReproved responseGatewayApproved = moviePaymentService.post(moviePayment);
        Assertions.assertEquals(responsePayment.getStatus(), responseGatewayApproved.getPaymentStatus());
        Assertions.assertEquals(responsePayment.getAuthorization().getReasonMessage(), responseGatewayApproved.getCause());

    }



    private UserEntity buildClientEntityWithCreditCardNumber(String creditCardNumber) {
        CreditCardEntity creditCard = CreditCardEntityBuilder.one().withCreditCardNumber(creditCardNumber).now();
        List<CreditCardEntity> creditCardEntityList = new ArrayList<>();
        creditCardEntityList.add(creditCard);
        return UserEntityBuilder.one().withCreditCard(creditCardEntityList).now();
    }

    private ResponsePayment buildResponsePaymentFailed() {
        ResponseProcessPayment responseProcessPayment = ResponseProcessPayment.builder()
                .reasonMessage("test fail")
                .build();

        return ResponsePayment.builder()
                .status("REPROVED")
                .authorization(responseProcessPayment)
                .build();
    }

    private ResponsePayment buildResponsePaymentApproved() {
        ResponseProcessPayment responseProcessPayment = ResponseProcessPayment.builder()
                .reasonMessage("approved")
                .build();

        return ResponsePayment.builder()
                .status("APPROVED")
                .authorization(responseProcessPayment)
                .build();
    }


    private ResponseMovieById buildResponseMovieById() {
        ResponseRentAndBuy responseRentAndBuy = ResponseRentAndBuy.builder()
                .price(50.0)
                .store(MovieLinks.NETFLIX.getLabel())
                .build();
        List<ResponseRentAndBuy> responseRentAndBuyList = new ArrayList<>();
        responseRentAndBuyList.add(responseRentAndBuy);

        ResponseJustWatch responseJustWatch = ResponseJustWatch.builder()
                .rent(responseRentAndBuyList)
                .buy(responseRentAndBuyList)
                .build();

        ResponseMovieById responseMovieById = ResponseMovieById.builder()
                .id(1L)
                .movieName("test")
                .justWatch(responseJustWatch)
                .build();

        return responseMovieById;
    }



}