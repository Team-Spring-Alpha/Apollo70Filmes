package br.com.compass.filmes.cliente.controller;

import br.com.compass.filmes.cliente.dto.apiMovie.ResponseApiMovieManagerDTO;
import br.com.compass.filmes.cliente.enums.GenresEnum;
import br.com.compass.filmes.cliente.enums.ProvidersEnum;
import br.com.compass.filmes.cliente.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/api/movie-manager")
public class MovieSearchController {
    @Autowired
    private MovieService service;

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<ResponseApiMovieManagerDTO>> getRecommendations(@PathVariable Long id) {
        List<ResponseApiMovieManagerDTO> responseApiMovieManagerDTOList = service.findMoviesRecommendations(id);
        return ResponseEntity.ok(responseApiMovieManagerDTOList);
    }

    @GetMapping("/movie-search")
    public ResponseEntity<List<ResponseApiMovieManagerDTO>> getMovieByFilters
            (@RequestParam(required = false, name = "movie_genrer") GenresEnum movieGenre,
             @RequestParam(required = false, name = "release_date_after") @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateGte,
             @RequestParam(required = false, name = "release_date_before") @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateLte,
             @RequestParam(required = false, name = "movie_provider") ProvidersEnum movieProvider,
             @RequestParam(required = false, name = "movie_peoples") List<String> moviePeoples,
             @RequestParam(required = false, name = "movie_name")String movieName) {
        List<ResponseApiMovieManagerDTO> responseApiMovieManagerDTO = service.findByFilters(movieGenre, dateGte, dateLte, movieProvider,
                                                                                        moviePeoples, movieName);
        return ResponseEntity.ok(responseApiMovieManagerDTO);
    }
}
