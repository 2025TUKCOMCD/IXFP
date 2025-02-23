package com.example.jzp.service;

import com.example.jzp.model.Movie;
import com.example.jzp.controller.MovieController;
import com.example.jzp.repository.MovieRepository;
import com.example.jzp.repository.TicketRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.jzp.model.Ticket;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


@Service

public class MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private static final String[] THEATER_NAMES = {"1관", "2관", "3관", "4관"};
    private static final Random RANDOM = new Random();

    @Autowired
    private TicketService ticketService; // TicketService 사용

    private static final int YOUTH_TICKET_PRICE = 10000;  // 청소년 가격
    private static final int ADULT_TICKET_PRICE = 15000;  // 성인 가격
    private static final int OLD_TICKET_PRICE = 8000;    // 노인 가격
    private static final int DISABLED_TICKET_PRICE = 5000; // 장애인 가격
    private final String API_KEY = "23da313eaaed21538b2ebab1161a0981";
    private static final String BASE_URL = "https://api.themoviedb.org/3/movie/popular";

    @Autowired
    public MovieService( MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<Movie> getMoviesByTime(Date movieCalendar, LocalTime movieTime) {
        // movieCalendar와 movieTime을 기반으로 티켓을 찾음
        List<Ticket> tickets = ticketRepository.findByMovieMovieCalendarAndMovieMovieTime(movieCalendar, movieTime);

        return tickets.stream()
                .map(Ticket::getMovie)  // Ticket에서 Movie를 추출
                .distinct()  // 중복된 영화가 있을 경우 제거
                .collect(Collectors.toList());
    }

    // 장르 목록을 가져와서 Map에 저장
    public Map<Integer, String> getGenreMap() {
        String url = "https://api.themoviedb.org/3/genre/movie/list?api_key=" + API_KEY + "&language=ko-KR";
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        Map<Integer, String> genreMap = new HashMap<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode genresNode = rootNode.path("genres");

            for (JsonNode genreNode : genresNode) {
                int id = genreNode.path("id").asInt();
                String name = genreNode.path("name").asText();
                genreMap.put(id, name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return genreMap;
    }

    // 영화 정보에서 장르 이름을 추출하는 메서드
    public List<String> extractGenres(JsonNode movieNode, Map<Integer, String> genreMap) {
        List<String> genres = new ArrayList<>();
        JsonNode genresNode = movieNode.path("genre_ids");

        for (JsonNode genreNode : genresNode) {
            int genreId = genreNode.asInt();  // 장르 ID
            String genreName = genreMap.get(genreId);  // 장르 이름 변환
            if (genreName != null) {
                genres.add(genreName);  // 리스트에 추가
            }
        }

        return genres;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveMoviesFromTMDB() {
        // TMDB API에서 영화 데이터 가져오기
        Map<Integer, String> genreMap = getGenreMap();
        String url = "https://api.themoviedb.org/3/movie/popular?api_key=" + API_KEY + "&language=ko-KR&page=1";
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode resultsNode = rootNode.path("results");

            int rating = 1;
            LocalDate today = LocalDate.now();  // 오늘 날짜 기준

            // 각 TMDB 영화 데이터 처리
            for (JsonNode movieNode : resultsNode) {
                Long tmdbMovieId = movieNode.path("id").asLong();
                String title = movieNode.path("title").asText();
                String posterPath = movieNode.path("poster_path").asText();
                List<String> genres = extractGenres(movieNode, genreMap);
                boolean adult = movieNode.path("adult").asBoolean();

                // 성인 여부와 장르를 기반으로 나이 등급을 결정
                String ageRating = getAgeRatingFromAdultAndGenres(adult, genres);

                Integer ranking = rating++;

                // 랜덤 시간 생성
                List<LocalTime> randomTimes = generateRandomTimes();  // 영화마다 랜덤 시간 생성


                for (int i = 0; i < 5; i++) {
                    LocalDate movieDate = today.plusDays(i);

                    // 각 상영 시간마다 새로운 Movie 객체 생성
                    for (LocalTime time : randomTimes) {
                        Movie movie = new Movie();  // 상영 시간마다 새로운 Movie 객체 생성
                        movie.setMovieId(UUID.randomUUID());  // 고유한 movie_id 생성
                        movie.setTmdbMovieId(tmdbMovieId);  // tmdbMovieId는 중복 가능
                        movie.setMovieName(title);
                        movie.setMovieImage("https://image.tmdb.org/t/p/w500" + posterPath);
                        movie.setMovieType(String.join(", ", genres));
                        movie.setMovieGrade(ageRating);
                        movie.setMovieRating(ranking);
                        movie.setMovieTime(time);  // 랜덤 시간 할당
                        movie.setMovieCalendar(java.sql.Date.valueOf(movieDate));  // 영화 상영일 설정
                        movie.setMovieSeatRemain(72);  // 영화의 좌석 수
                        movie.setMovieTheater(THEATER_NAMES[RANDOM.nextInt(THEATER_NAMES.length)]);  // 랜덤 영화관

                        // Movie DB에 저장
                        movieRepository.save(movie);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private List<LocalTime> generateRandomTimes() {
        LocalTime start = LocalTime.parse("08:20", DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime end = LocalTime.parse("23:55", DateTimeFormatter.ofPattern("HH:mm"));


        int startMinutes = start.getHour() * 60 + start.getMinute();
        int endMinutes = end.getHour() * 60 + end.getMinute();


        int totalMinutes = endMinutes - startMinutes;
        int totalIntervals = totalMinutes / 5;


        Set<LocalTime> randomTimesSet = new HashSet<>();
        while (randomTimesSet.size() < 4) {

            int randomOffset = RANDOM.nextInt(totalIntervals + 1) * 5;
            LocalTime randomTime = start.plusMinutes(randomOffset);
            randomTimesSet.add(randomTime);
        }

        List<LocalTime> randomTimes = new ArrayList<>(randomTimesSet);
        randomTimes.sort(Comparator.naturalOrder());

        return randomTimes;
    }



    // 나이 등급을 결정하는 메서드
    private String getAgeRatingFromAdultAndGenres(boolean adult, List<String> genres) {
        // 성인 여부가 true이면 19세 미만 관람 불가
        if (adult) {
            return "19세 미만 관람 불가";
        }

        // 15세 이상 나이 등급을 결정할 장르들
        List<String> ageRestrictedGenres15 = List.of("로맨스", "스릴러", "공포", "범죄", "미스터리", "전쟁");

        // 15세 이상 장르가 있으면 15세 이상으로 설정
        for (String genre : genres) {
            if (ageRestrictedGenres15.contains(genre)) {
                return "15세 이상";
            }
        }

        // 해당 장르가 없으면 전체이용가로 설정
        return "전체이용가";
    }


    // 날짜별 영화 조회
    public List<MovieController.MovieResponse> getMoviesByDate(Date movieCalendar) {
        List<Movie> movies = movieRepository.findByMovieCalendar(movieCalendar);
        return movies.stream().map(movie -> {
            MovieController.MovieResponse response = new MovieController.MovieResponse();
            response.setMovieId(movie.getMovieId());
            response.setMovieImage(movie.getMovieImage());
            response.setMovieName(movie.getMovieName());
            response.setMovieType(movie.getMovieType());
            response.setMovieRating(movie.getMovieRating());
            response.setMovieGrade(movie.getMovieGrade());
            response.setMovieTime(movie.getMovieTime());  // LocalTime 사용
            response.setMovieSeatRemain(movie.getMovieSeatRemain());
            response.setMovieTheater(movie.getMovieTheater());
            return response;
        }).collect(Collectors.toList());
    }

    // 영화 시간과 극장 정보 업데이트
        public boolean updateMovieTime(UUID movieId, LocalTime movieTime, String movieTheater) {
            // 영화 ID로 Movie 객체를 조회
            Optional<Movie> movieOptional = movieRepository.findById(movieId);
            if (movieOptional.isEmpty()) {
                return false;  // 영화 정보가 없으면 false 반환
            }

            Movie movie = movieOptional.get();

            // 해당 영화와 관련된 모든 티켓 조회
            List<Ticket> tickets = ticketRepository.findByMovie(movie);
            if (tickets.isEmpty()) {
                // 티켓이 없다면 새로 생성
                Ticket newTicket = createNewTicket(movie, movieTime, movieTheater);
                ticketRepository.save(newTicket);  // 새 티켓 저장
            } else {
                // 기존 티켓은 업데이트하지 않음 (필요한 경우 업데이트 가능)
                for (Ticket ticket : tickets) {
                    ticket.setMovieTime(movieTime);
                    ticket.setMovieTheater(movieTheater);
                    ticketRepository.save(ticket);  // 각 티켓 저장
                }
            }

            return true;
        }

    private Ticket createNewTicket(Movie movie, LocalTime movieTime, String movieTheater) {
        Ticket ticket = new Ticket();
        ticket.setMovie(movie);
        ticket.setMovieTime(movieTime);
        ticket.setMovieTheater(movieTheater);

        return ticket;
    }

    // 영화에 해당하는 티켓 정보 업데이트
    private void updateTicketsForMovie(Movie movie) {
        List<Ticket> tickets = ticketRepository.findByMovie(movie);
        for (Ticket ticket : tickets) {
            ticket.setMovieTheater(movie.getMovieTheater());
            ticket.setMovieTime(movie.getMovieTime());
            ticketRepository.save(ticket);
        }
    }

    public boolean setMovieSeat(UUID movieId, String movieSeat, String movieTheater, String movieName, String movieTime) {
        Optional<Movie> movieOptional = movieRepository.findById(movieId);
        if (movieOptional.isEmpty()) {
            return false; // 영화 정보가 없으면 실패
        }

        Movie movie = movieOptional.get();

        LocalTime requestedTime = LocalTime.parse(movieTime);
        if (!movie.getMovieName().equals(movieName) || !movie.getMovieTime().equals(requestedTime)) {
            return false;
        }

        String currentReservedSeats = movie.getMovieSeat();
        List<String> reservedSeatsList = new ArrayList<>(Arrays.asList(currentReservedSeats.split(",")));

        String[] requestedSeats = movieSeat.split(",");
        for (String seat : requestedSeats) {
            if (reservedSeatsList.contains(seat)) {
                return false; // 이미 예약된 좌석이면 실패
            }
            reservedSeatsList.add(seat); // 새로운 좌석 추가
        }

        int reservedCount = requestedSeats.length;
        int remainingSeats = movie.getMovieSeatRemain();

        if (remainingSeats < reservedCount) {
            return false; // 좌석 부족
        }

        movie.setMovieSeatRemain(remainingSeats - reservedCount);

        // 리스트를 결합하기 전에 불필요한 공백 및 빈 요소 제거
        reservedSeatsList.removeIf(String::isEmpty);  // 빈 문자열이 있으면 제거

        // 결합된 좌석 정보 업데이트
        movie.setMovieSeat(String.join(",", reservedSeatsList));
        movieRepository.save(movie); // DB에 반영

        // 최근 티켓을 찾아서 좌석을 업데이트
        Optional<Ticket> latestTicketOptional = ticketRepository.findTopByMovieOrderByCreatedAtDesc(movie);
        if (latestTicketOptional.isPresent()) {
            Ticket latestTicket = latestTicketOptional.get();
            latestTicket.setMovieSeat(movieSeat); // 입력된 좌석 정보로 업데이트
            latestTicket.setMovieTheater(movieTheater); // 상영관 정보 업데이트
            ticketRepository.save(latestTicket); // 변경된 티켓 저장
        }

        return true;
    }






    public String getUpdatedMovieSeat(UUID movieId) {
        Optional<Movie> movieOptional = movieRepository.findById(movieId);
        if (movieOptional.isPresent()) {
            Movie movie = movieOptional.get();
            return movie.getMovieSeat();
        }
        return "";
    }




    // 남은 좌석 수 조회
    public int getMovieSeatRemain(UUID movieId) {
        return ticketService.getMovieSeatRemain(movieId);
    }

    public boolean saveMovieCustomer(UUID movieId, int disabled, int youth, int adult, int old) {
        // TicketService의 saveCustomerTicket 호출
        return ticketService.saveCustomerTicket(movieId, disabled, youth, adult, old);
    }


    // 결제 내역 조회
    public Map<String, Object> getPaymentHistory() {
        // 결제 내역 조회
        List<Ticket> tickets = ticketRepository.findAll(); // 조건에 맞는 티켓 조회 가능

        // 응답을 위한 데이터 맵 생성
        Map<String, Object> response = new HashMap<>();

        // 총 결제 금액 초기화
        int totalPrice = 0;

        // 영화 정보와 가격 정보 목록 생성
        List<Map<String, Object>> movieHistoryList = new ArrayList<>();

        // 각 티켓에 대해 필요한 정보를 처리
        for (Ticket ticket : tickets) {
            Map<String, Object> movieHistory = new HashMap<>();

            // 영화 정보 설정
            Movie movie = ticket.getMovie(); // Ticket 객체에서 Movie 정보 가져오기
            Map<String, Object> movieInfo = new HashMap<>();
            movieInfo.put("movieId", movie.getMovieId());
            movieInfo.put("movieImage", movie.getMovieImage());
            movieInfo.put("movieName", movie.getMovieName());
            movieInfo.put("movieType", movie.getMovieType());
            movieInfo.put("movieRating", movie.getMovieRating());
            movieInfo.put("movieTime", movie.getMovieTime());
            movieInfo.put("movieSeatRemain", movie.getMovieSeatRemain());
            movieInfo.put("movieTheater", movie.getMovieTheater());
            movieInfo.put("movieGrade", movie.getMovieGrade());

            movieHistory.put("movie", movieInfo);

            // 고객 정보 설정
            Map<String, Integer> movieCustomerInfo = new HashMap<>();
            movieCustomerInfo.put("movieCustomerDisabled", ticket.getCustomerDisabled());
            movieCustomerInfo.put("movieCustomerYouth", ticket.getCustomerYouth());
            movieCustomerInfo.put("movieCustomerAdult", ticket.getCustomerAdult());
            movieCustomerInfo.put("movieCustomerOld", ticket.getCustomerOld());

            movieHistory.put("movieCustomer", movieCustomerInfo);

            // 가격 계산 (인원수 * 가격)
            int youthPrice = ticket.getCustomerYouth() * YOUTH_TICKET_PRICE;
            int adultPrice = ticket.getCustomerAdult() * ADULT_TICKET_PRICE;
            int oldPrice = ticket.getCustomerOld() * OLD_TICKET_PRICE;
            int disabledPrice = ticket.getCustomerDisabled() * DISABLED_TICKET_PRICE;

            Map<String, Integer> priceInfo = new HashMap<>();
            priceInfo.put("youthPrice", youthPrice);
            priceInfo.put("adultPrice", adultPrice);
            priceInfo.put("oldPrice", oldPrice);
            priceInfo.put("disabledPrice", disabledPrice);

            movieHistory.put("price", priceInfo);
            movieHistory.put("ticketId", ticket.getTicketId());

            // 총 금액 계산
            totalPrice += youthPrice + adultPrice + oldPrice + disabledPrice;

            movieHistoryList.add(movieHistory);
        }

        // 전체 응답에 영화 내역과 총 금액 추가
        response.put("movieHistory", movieHistoryList);
        response.put("totalPrice", totalPrice);

        return response;
    }

    public Map<String, Object> getTicketDetails(UUID ticketId) {
        Optional<Ticket> ticketOptional = ticketRepository.findById(ticketId);

        if (ticketOptional.isEmpty()) {
            return null; // 예매 정보가 없으면 null 반환
        }

        Ticket ticket = ticketOptional.get();
        Movie movie = ticket.getMovie();

        Map<String, Object> response = new HashMap<>();

        // 영화 정보
        Map<String, Object> movieInfo = new HashMap<>();
        movieInfo.put("movieId", movie.getMovieId());
        movieInfo.put("movieImage", movie.getMovieImage());
        movieInfo.put("movieName", movie.getMovieName());
        movieInfo.put("movieType", movie.getMovieType());
        movieInfo.put("movieRating", movie.getMovieRating());
        movieInfo.put("movieTime", movie.getMovieTime());
        movieInfo.put("movieSeatRemain", movie.getMovieSeatRemain());
        movieInfo.put("movieTheater", movie.getMovieTheater());
        movieInfo.put("movieGrade", movie.getMovieGrade());

        response.put("movie", movieInfo);

        // 고객 정보
        Map<String, Integer> movieCustomerInfo = new HashMap<>();
        movieCustomerInfo.put("movieCustomerAdult", ticket.getCustomerAdult());
        movieCustomerInfo.put("movieCustomerDisabled", ticket.getCustomerDisabled());
        movieCustomerInfo.put("movieCustomerYouth", ticket.getCustomerYouth());
        movieCustomerInfo.put("movieCustomerOld", ticket.getCustomerOld());

        response.put("movieCustomer", movieCustomerInfo);

        // 티켓 정보
        response.put("ticketId", ticket.getTicketId());

        return response;
    }

}
