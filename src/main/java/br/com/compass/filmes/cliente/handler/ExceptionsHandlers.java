package br.com.compass.filmes.cliente.handler;

import br.com.compass.filmes.cliente.exceptions.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class ExceptionsHandlers {

    @Autowired
    private MessageSource messageSource;

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ExceptionResponseDto>> handleInvalidArgument(MethodArgumentNotValidException exception) {
        List<ExceptionResponseDto> responseDTOList = new ArrayList<>();
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        fieldErrors.forEach(e -> {
            String message = messageSource.getMessage(e, LocaleContextHolder.getLocale());
            ExceptionResponseDto error = new ExceptionResponseDto(e.getField(), message);
            responseDTOList.add(error);
        });
        return ResponseEntity.badRequest().body(responseDTOList);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionResponseDto> handleHttpValidationExceptions(HttpMessageNotReadableException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorMessage = "Invalid information";

        if (e.getCause() instanceof InvalidFormatException) {
            InvalidFormatException cause = (InvalidFormatException) e.getCause();
            errorMessage = cause.getCause().getMessage();
        }
        return ResponseEntity.status(status).body(new ExceptionResponseDto(String.valueOf(status.value()), errorMessage));

    }

    @ExceptionHandler(CreditCardNotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handlerCreditCardNotFoundException(CreditCardNotFoundException creditCardNotFoundException) {
        ExceptionResponseDto responseDto = new ExceptionResponseDto(creditCardNotFoundException.getMessage(), "credit_card_number");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto);
    }

    @ExceptionHandler(RentAndBuyMoviesEmptyException.class)
    public ResponseEntity<ExceptionResponseDto> handlerRentAndBuyMoviesEmptyException(RentAndBuyMoviesEmptyException rentAndBuyMoviesEmptyException) {
        ExceptionResponseDto responseDto = new ExceptionResponseDto( "movies buy list and rent list is empty. Either list must be filled", "movies");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handlerClientNotFoundException(UserNotFoundException userNotFoundException) {
        ExceptionResponseDto responseDto = new ExceptionResponseDto(userNotFoundException.getMessage(), "user_id");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto);
    }

    @ExceptionHandler(BuyMovieNotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handlerBuyMovieNotFoundException(BuyMovieNotFoundException buyMovieNotFoundException) {
        ExceptionResponseDto responseDto = new ExceptionResponseDto(buyMovieNotFoundException.getMessage(), "movie.buy");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto);
    }

    @ExceptionHandler(RentMovieNotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handlerRentMovieNotFoundException(RentMovieNotFoundException rentMovieNotFoundException) {
        ExceptionResponseDto responseDto = new ExceptionResponseDto(rentMovieNotFoundException.getMessage(), "movie.rent");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto);
    }

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handlerMovieNotFoundException(MovieNotFoundException movieNotFoundException) {
        ExceptionResponseDto responseDto = new ExceptionResponseDto(movieNotFoundException.getMessage(), "movie");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDto);
    }


}