package br.com.compass.filmes.cliente.service;

import br.com.compass.filmes.cliente.dto.apiAllocationHistory.RequestAllocation;
import br.com.compass.filmes.cliente.dto.apiAllocationHistory.RequestAllocationMovie;
import br.com.compass.filmes.cliente.dto.apiPayment.request.RequestPayment;
import br.com.compass.filmes.cliente.dto.apiPayment.request.RequestPaymentCreditCard;
import br.com.compass.filmes.cliente.dto.apiPayment.request.RequestPaymentCustomer;
import br.com.compass.filmes.cliente.dto.apiPayment.response.*;
import br.com.compass.filmes.cliente.dto.apiMovieManager.RequestMoviePayment;
import br.com.compass.filmes.cliente.dto.client.response.apiMovie.ResponseMovieById;
import br.com.compass.filmes.cliente.entities.ClientEntity;
import br.com.compass.filmes.cliente.entities.CreditCardEntity;
import br.com.compass.filmes.cliente.enums.ClientEnum;
import br.com.compass.filmes.cliente.enums.MovieLinks;
import br.com.compass.filmes.cliente.proxy.GatewayProxy;
import br.com.compass.filmes.cliente.proxy.MovieSearchProxy;
import br.com.compass.filmes.cliente.rabbitMq.MessageHistory;
import br.com.compass.filmes.cliente.repository.ClientRepository;
import br.com.compass.filmes.cliente.util.Md5;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MoviePaymentService {

    private LocalTime tokenExpirationTime;
    private String authToken;
    private final ModelMapper modelMapper;
    private final ClientRepository clientRepository;
    private final MovieSearchProxy movieSearchProxy;
    private final GatewayProxy gatewayProxy;
    private final MessageHistory messageHistory;
    private final Md5 md5;

    public ResponseGatewayReproved post(RequestMoviePayment requestMoviePayment) {
        ClientEntity clientEntity = clientRepository.findById(requestMoviePayment.getUserId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        CreditCardEntity creditCard = getCreditCard(requestMoviePayment, clientEntity);

        List<ResponseMoviePaymentProcess> moviePaymentProcessList = new ArrayList<>();
        Double amount = 0.0;

        if (requestMoviePayment.getMovies().getBuy() != null) {
            for (int i = 0; i < requestMoviePayment.getMovies().getBuy().size(); i++) {
                ResponseMovieById proxyMovieById = movieSearchProxy.getMovieById(requestMoviePayment.getMovies().getBuy().get(i));
                try {
                    Double buyPrice = proxyMovieById.getJustWatch().getBuy().get(0).getPrice();
                    amount += buyPrice;
                    buildMoviesProcessBuy(requestMoviePayment, moviePaymentProcessList, i, proxyMovieById);

                } catch (NullPointerException nullPointerException) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                }
            }
        }

        if (requestMoviePayment.getMovies().getRent() != null) {
            for (int i = 0; i < requestMoviePayment.getMovies().getRent().size(); i++) {
                ResponseMovieById proxyMovieById = movieSearchProxy.getMovieById(requestMoviePayment.getMovies().getRent().get(i));
                try {
                    Double rentPrice = proxyMovieById.getJustWatch().getRent().get(0).getPrice();
                    amount += rentPrice;
                    buildMoviesProcessRent(requestMoviePayment, moviePaymentProcessList, i, proxyMovieById);

                } catch (NullPointerException nullPointerException) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                }
            }
        }


        ClientEnum randomClientEnum = ClientEnum.getRandomClientEnum();
        if (this.tokenExpirationTime == null) {
            getToken(randomClientEnum);
        }

        if (LocalTime.now().isAfter(tokenExpirationTime)) {
            getToken(randomClientEnum);
        }


        RequestPaymentCustomer paymentCustomer = buildCustomer(clientEntity);

        RequestPayment requestPayment = buildRequesPayment(creditCard, amount, randomClientEnum, paymentCustomer);

        ResponsePayment payment = gatewayProxy.getPayment(this.authToken, requestPayment);

        List<RequestAllocationMovie> requestAllocationMovieList = getRequestAllocationMovies(moviePaymentProcessList);
        RequestAllocation requestAllocation = buildRequestAllocation(requestMoviePayment, clientEntity, payment, requestAllocationMovieList);
        messageHistory.sendMessage(requestAllocation);

        if (payment.getStatus().equals("REPROVED")) {
            ResponseGatewayReproved response = new ResponseGatewayReproved();
            response.setCause(payment.getAuthorization().getReasonMessage());
            return response;
        }

        ResponseGatewayOk responseGatewayOk = new ResponseGatewayOk();
        responseGatewayOk.setMovies(moviePaymentProcessList);
        responseGatewayOk.setPaymentStatus("APPROVED");
        responseGatewayOk.setCause("approved");
        return responseGatewayOk;
    }

    private MovieLinks getMovieLinkFromlabel(String label) {
        return MovieLinks.valueOfLabel(label);
    }

    private void buildMoviesProcessBuy(RequestMoviePayment requestMoviePayment, List<ResponseMoviePaymentProcess> moviePaymentProcessList, int i, ResponseMovieById proxyMovieById) {
        ResponseMoviePaymentProcess moviePaymentProcess = new ResponseMoviePaymentProcess();
        moviePaymentProcess.setMovieId(requestMoviePayment.getMovies().getBuy().get(i));
        moviePaymentProcess.setTitle(proxyMovieById.getMovieName());
        MovieLinks movieLinks = getMovieLinkFromlabel(proxyMovieById.getJustWatch().getBuy().get(0).getStore());
        moviePaymentProcess.setLink(movieLinks.getLink());
        moviePaymentProcessList.add(moviePaymentProcess);
    }

    private void buildMoviesProcessRent(RequestMoviePayment requestMoviePayment, List<ResponseMoviePaymentProcess> moviePaymentProcessList, int i, ResponseMovieById proxyMovieById) {
        ResponseMoviePaymentProcess moviePaymentProcess = new ResponseMoviePaymentProcess();
        moviePaymentProcess.setMovieId(requestMoviePayment.getMovies().getRent().get(i));
        moviePaymentProcess.setTitle(proxyMovieById.getMovieName());
        MovieLinks movieLinks = getMovieLinkFromlabel(proxyMovieById.getJustWatch().getRent().get(0).getStore());
        moviePaymentProcess.setLink(movieLinks.getLink());
        moviePaymentProcessList.add(moviePaymentProcess);
    }

    private List<RequestAllocationMovie> getRequestAllocationMovies(List<ResponseMoviePaymentProcess> response) {
        List<RequestAllocationMovie> requestAllocationMovieList = new ArrayList<>();
        for (ResponseMoviePaymentProcess responseMoviePaymentProcess : response) {
            RequestAllocationMovie allocationMovie = new RequestAllocationMovie(responseMoviePaymentProcess.getMovieId(), responseMoviePaymentProcess.getTitle());
            requestAllocationMovieList.add(allocationMovie);
        }
        return requestAllocationMovieList;
    }

    private RequestAllocation buildRequestAllocation(RequestMoviePayment requestMoviePayment, ClientEntity clientEntity, ResponsePayment payment, List<RequestAllocationMovie> requestAllocationMovieList) {
        RequestAllocation requestAllocation = new RequestAllocation();
        requestAllocation.setMovies(requestAllocationMovieList);
        requestAllocation.setPaymentStatus(payment.getStatus());
        requestAllocation.setCardNumber(requestMoviePayment.getCreditCardNumber());
        requestAllocation.setUserId(clientEntity.getId());
        return requestAllocation;
    }

    private void getToken(ClientEnum randomClientEnum) {
        ResponseAuth authToken = gatewayProxy.getAuthToken(randomClientEnum);
        this.tokenExpirationTime = LocalTime.now().plusSeconds(Long.parseLong(authToken.getExpiresIn()));
        this.authToken = authToken.getToken();
    }

    private RequestPayment buildRequesPayment(CreditCardEntity creditCard, Double amount, ClientEnum randomClientEnum, RequestPaymentCustomer paymentCustomer) {
        RequestPayment requestPayment = new RequestPayment();
        requestPayment.setSellerId(randomClientEnum.getSellerId());
        requestPayment.setCustomer(paymentCustomer);
        requestPayment.setTransactionAmount(amount);
        RequestPaymentCreditCard requestCreditCard = modelMapper.map(creditCard, RequestPaymentCreditCard.class);
        requestCreditCard.setClientCreditCardNumber(md5.ToMd5(creditCard.getClientCreditCardNumber()));
        requestPayment.setCard(requestCreditCard);
        return requestPayment;
    }

    private CreditCardEntity getCreditCard(RequestMoviePayment requestMoviePayment, ClientEntity clientEntity) {
        for (int i = 0; i < clientEntity.getCreditCards().size(); i++) {
            if (Objects.equals(clientEntity.getCreditCards().get(i).getClientCreditCardNumber(), requestMoviePayment.getCreditCardNumber())) {
                return clientEntity.getCreditCards().get(i);
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private RequestPaymentCustomer buildCustomer(ClientEntity clientEntity) {
        RequestPaymentCustomer paymentCustomer = new RequestPaymentCustomer();
        paymentCustomer.setDocumentNumber(clientEntity.getClientCpf());
        return paymentCustomer;
    }


}
