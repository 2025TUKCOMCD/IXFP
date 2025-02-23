package com.example.jzp.controller;

import com.example.jzp.model.Movie;
import com.example.jzp.service.MovieService;
import com.example.jzp.service.TicketService;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;
import java.util.stream.Collectors;
import java.util.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/movie")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private TicketService ticketService;

    @GetMapping("/save-tmdb")
    public String saveMovies() {
        movieService.saveMoviesFromTMDB(); // TMDB에서 영화 데이터를 가져오고 저장
        return "Movies saved successfully!";
    }

    // 영화 그룹별 요청
    @PostMapping("/showmovie/{group}")
    public ResponseEntity<?> showMovieByGroup(@PathVariable("group") String group,
                                              @RequestBody MovieCalendarRequest request) {
        // 영화 리스트를 요청된 날짜로 가져옴
        List<MovieResponse> movies = movieService.getMoviesByDate(request.getMovieCalendar());

        // 사용자 그룹에 맞는 우선순위 처리
        if ("youth".equals(group)) {
            movies = prioritizeForYouth(movies); // 청소년 우선순위 정렬
        } else if ("old".equals(group)) {
            movies = prioritizeForOld(movies); // 노인 우선순위 정렬
        }

        return ResponseEntity.ok(Map.of(
                "movieCalendar", request.getMovieCalendar(),
                "movies", movies
        ));
    }

    // 청소년 우선순위로 영화 정렬
    private List<MovieResponse> prioritizeForYouth(List<MovieResponse> movies) {
        return movies.stream()
                .sorted(Comparator.comparingInt(MovieResponse::getMovieRating)) // 영화 평점으로 우선 정렬
                .collect(Collectors.toList());
    }

    // 노인 우선순위로 영화 정렬, 드라마 장르를 우선시
    private List<MovieResponse> prioritizeForOld(List<MovieResponse> movies) {
        return movies.stream()
                .sorted(Comparator.comparingInt((MovieResponse movie) ->
                                movie.getMovieType().equals("드라마") ? 1 : 0) // 드라마 영화를 먼저
                        .reversed() // 드라마 우선
                        .thenComparingInt(MovieResponse::getMovieRating)) // 영화 평점으로 정렬
                .collect(Collectors.toList());
    }

    // DTO: 영화 날짜 요청
    public static class MovieCalendarRequest {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date movieCalendar;

        public Date getMovieCalendar() {
            return movieCalendar;
        }

        public void setMovieCalendar(Date movieCalendar) {
            this.movieCalendar = movieCalendar;
        }
    }

    // 영화 시간 저장
    @PostMapping("/time")
    public ResponseEntity<?> updateMovieTime(@RequestBody MovieTimeRequest request) {
        boolean success = movieService.updateMovieTime(request.getMovieId(), request.getMovieTime(), request.getMovieTheater());

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "failed",
                    "message", "영화 정보를 찾을 수 없습니다."
            ));
        }

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    public static class MovieTimeRequest {
        private UUID movieId;
        private LocalTime movieTime;
        private String movieTheater;

        // Getters and Setters
        public UUID getMovieId() {
            return movieId;
        }

        public void setMovieId(UUID movieId) {
            this.movieId = movieId;
        }

        public LocalTime getMovieTime() {
            return movieTime;
        }

        public void setMovieTime(LocalTime movieTime) {
            this.movieTime = movieTime;
        }

        public String getMovieTheater() {
            return movieTheater;
        }

        public void setMovieTheater(String movieTheater) {
            this.movieTheater = movieTheater;
        }
    }

    // DTO: 영화 응답 데이터
    public static class MovieResponse {
        private UUID movieId;
        private String movieImage;
        private String movieName;
        private String movieType;
        private int movieRating;
        private LocalTime movieTime;
        private int movieSeatRemain;
        private String movieTheater;
        private String movieGrade;

        // Getters and Setters
        public UUID getMovieId() {
            return movieId;
        }

        public void setMovieId(UUID movieId) {
            this.movieId = movieId;
        }

        public String getMovieImage() {
            return movieImage;
        }

        public void setMovieImage(String movieImage) {
            this.movieImage = movieImage;
        }

        public String getMovieName() {
            return movieName;
        }

        public void setMovieName(String movieName) {
            this.movieName = movieName;
        }

        public String getMovieType() {
            return movieType;
        }

        public void setMovieType(String movieType) {
            this.movieType = movieType;
        }

        public int getMovieRating() {
            return movieRating;
        }

        public void setMovieRating(int movieRating) {
            this.movieRating = movieRating;
        }

        public LocalTime getMovieTime() {
            return movieTime;
        }

        public void setMovieTime(LocalTime movieTime) {
            this.movieTime = movieTime;
        }

        public int getMovieSeatRemain() {
            return movieSeatRemain;
        }

        public void setMovieSeatRemain(int movieSeatRemain) {
            this.movieSeatRemain = movieSeatRemain;
        }

        public String getMovieGrade() {
            return movieGrade;
        }

        public void setMovieGrade(String movieGrade) {
            this.movieGrade = movieGrade;
        }

        public String getMovieTheater() {
            return movieTheater;
        }

        public void setMovieTheater(String movieTheater) {
            this.movieTheater = movieTheater;
        }
    }

    @PostMapping("/seat")
    public ResponseEntity<?> setMovieSeat(@RequestBody MovieSeatRequest request) {
        if (request.getMovieId() == null || request.getMovieSeat() == null || request.getMovieName() == null || request.getMovieTime() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "필수 항목이 누락되었습니다"
            ));
        }

        UUID movieId = request.getMovieId();
        String movieSeat = request.getMovieSeat();
        String movieTheater = request.getMovieTheater();
        String movieName = request.getMovieName();  // 영화 이름
        String movieTime = request.getMovieTime();  // 영화 시간

        boolean success = movieService.setMovieSeat(movieId, movieSeat, movieTheater, movieName, movieTime); // Pass all the required parameters

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "해당 좌석을 이용할 수 없습니다"
            ));
        }

        // 예약 후 최신 좌석 정보 조회
        int remainingSeats = movieService.getMovieSeatRemain(movieId);
        String updatedSeats = movieService.getUpdatedMovieSeat(movieId);  // Get updated movie seat info

        return ResponseEntity.ok(Map.of(
                "success", true,
                "movieId", movieId,
                "movieSeatRemain", remainingSeats
        ));
    }



    // DTO: 영화 좌석 저장 요청
    public static class MovieSeatRequest {
        private UUID movieId;
        private String movieSeat;
        private String movieTheater;
        private String movieName;  // 영화 이름 추가
        private String movieTime;

        // Getters and Setters
        public UUID getMovieId() {
            return movieId;
        }

        public void setMovieId(UUID movieId) {
            this.movieId = movieId;
        }

        public String getMovieSeat() {
            return movieSeat;
        }

        public void setMovieSeat(String movieSeat) {
            this.movieSeat = movieSeat;
        }

        public String getMovieTheater() {
            return movieTheater;
        }

        public void setMovieTheater(String movieTheater) {
            this.movieTheater = movieTheater;
        }

        public String getMovieName() {
            return movieName;
        }

        public void setMovieName(String movieName) {
            this.movieName = movieName;
        }

        public String getMovieTime() {
            return movieTime;
        }

        public void setMovieTime(String movieTime) {
            this.movieTime = movieTime;
        }
    }

    @PostMapping("/customer")
    public ResponseEntity<?> setMovieCustomer(@RequestBody MovieCustomerRequest request) {
        if (request.getMovieId() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "필수 항목이 누락되었습니다"
            ));
        }

        boolean success = movieService.saveMovieCustomer(
                request.getMovieId(),
                request.getMovieCustomerDisabled(),
                request.getMovieCustomerYouth(),
                request.getMovieCustomerAdult(),
                request.getMovieCustomerOld()
        );

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true
        ));
    }

    public static class MovieCustomerRequest {
        private UUID movieId;
        private int movieCustomerDisabled;
        private int movieCustomerYouth;
        private int movieCustomerAdult;
        private int movieCustomerOld;

        public UUID getMovieId() {
            return movieId;
        }

        public void setMovieId(UUID movieId) {
            this.movieId = movieId;
        }

        public int getMovieCustomerDisabled() {
            return movieCustomerDisabled;
        }

        public void setMovieCustomerDisabled(int movieCustomerDisabled) {
            this.movieCustomerDisabled = movieCustomerDisabled;
        }

        public int getMovieCustomerYouth() {
            return movieCustomerYouth;
        }

        public void setMovieCustomerYouth(int movieCustomerYouth) {
            this.movieCustomerYouth = movieCustomerYouth;
        }

        public int getMovieCustomerAdult() {
            return movieCustomerAdult;
        }

        public void setMovieCustomerAdult(int movieCustomerAdult) {
            this.movieCustomerAdult = movieCustomerAdult;
        }

        public int getMovieCustomerOld() {
            return movieCustomerOld;
        }

        public void setMovieCustomerOld(int movieCustomerOld) {
            this.movieCustomerOld = movieCustomerOld;
        }
    }

    @GetMapping("/ticket")
    public ResponseEntity<Map<String, Object>> getTicketDetails(@RequestParam("ticketId") UUID ticketId){
        Map<String, Object> ticketDetails = movieService.getTicketDetails(ticketId);

        if (ticketDetails != null) {
            return ResponseEntity.ok(ticketDetails);
        }
        return ResponseEntity.notFound().build();
    }

    public static class TicketRequest {
        private UUID ticketId;

        public UUID getTicketId() {
            return ticketId;
        }

        public void setTicketId(UUID ticketId) {
            this.ticketId = ticketId;
        }
    }

    @GetMapping("/banner")
    public ResponseEntity<?> getBanner() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "movieImage", "http://3.106.89.95/images/harry2.jpg"
        ));
    }

    // 새로운 응답 클래스 정의
    public static class MovieTimeResponse {
        private UUID movieId;
        private String movieImage;
        private String movieName;
        private String movieType;
        private int movieRating;
        private LocalTime movieTime;
        private String movieTheater;
        private String movieGrade;
        private String movieSeat;
        private int movieSeatRemain;

        public MovieTimeResponse(UUID movieId, String movieImage, String movieName,
                                 String movieType, int movieRating, LocalTime movieTime,
                                 String movieTheater, String movieGrade,String movieSeat, int movieSeatRemain) {
            this.movieId = movieId;
            this.movieImage = movieImage;
            this.movieName = movieName;
            this.movieType = movieType;
            this.movieRating = movieRating;
            this.movieTime = movieTime;
            this.movieTheater = movieTheater;
            this.movieGrade = movieGrade;
            this.movieSeat = movieSeat;
            this.movieSeatRemain = movieSeatRemain;
        }

        // Getters and Setters
        public UUID getMovieId() {
            return movieId;
        }

        public void setMovieId(UUID movieId) {
            this.movieId = movieId;
        }

        public String getMovieImage() {
            return movieImage;
        }

        public void setMovieImage(String movieImage) {
            this.movieImage = movieImage;
        }

        public String getMovieName() {
            return movieName;
        }

        public void setMovieName(String movieName) {
            this.movieName = movieName;
        }

        public String getMovieType() {
            return movieType;
        }

        public void setMovieType(String movieType) {
            this.movieType = movieType;
        }

        public int getMovieRating() {
            return movieRating;
        }

        public void setMovieRating(int movieRating) {
            this.movieRating = movieRating;
        }

        public LocalTime getMovieTime() {
            return movieTime;
        }

        public void setMovieTime(LocalTime movieTime) {
            this.movieTime = movieTime;
        }

        public String getMovieTheater() {
            return movieTheater;
        }

        public void setMovieTheater(String movieTheater) {
            this.movieTheater = movieTheater;
        }

        public String getMovieGrade() {
            return movieGrade;
        }

        public void setMovieGrade(String movieGrade) {
            this.movieGrade = movieGrade;
        }
        public String getMovieSeat() {
            return movieSeat;
        }

        public void setMovieSeat(String movieSeat) {
            this.movieSeat = movieSeat;
        }

        public int getMovieSeatRemain() {
            return movieSeatRemain;
        }

        public void setMovieSeatRemain(int movieSeatRemain) {
            this.movieSeatRemain = movieSeatRemain;
        }
    }

    @GetMapping("/movietime")
    public List<MovieTimeResponse> getMoviesByTime(
            @RequestParam(name = "movieCalendar") @DateTimeFormat(pattern = "yyyy-MM-dd") Date movieCalendar,
            @RequestParam(name = "movieTime") @DateTimeFormat(pattern = "HH:mm:ss") LocalTime movieTime) {

        List<Movie> movies = movieService.getMoviesByTime(movieCalendar, movieTime);

        return movies.stream()
                .map(movie -> new MovieTimeResponse(
                        movie.getMovieId(),
                        movie.getMovieImage(),
                        movie.getMovieName(),
                        movie.getMovieType(),
                        movie.getMovieRating(),
                        movie.getMovieTime(),
                        movie.getMovieTheater(),
                        movie.getMovieGrade(),
                        movie.getMovieSeat(),
                        movie.getMovieSeatRemain()))

                .collect(Collectors.toList());
    }

        // 결제 내역 확인
        @GetMapping("/payment/history")
        public ResponseEntity<Map<String, Object>> showPayment() {
            // MovieService에서 결제 내역과 총 금액 처리
            Map<String, Object> paymentHistory = movieService.getPaymentHistory();

            return ResponseEntity.ok(paymentHistory);
        }


}
