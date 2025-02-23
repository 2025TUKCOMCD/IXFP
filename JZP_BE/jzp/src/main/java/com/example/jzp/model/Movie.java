package com.example.jzp.model;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
public class Movie {

    @Id
    private UUID movieId;
    private String movieName;
    private Long tmdbMovieId;


    @Temporal(TemporalType.DATE)
    private Date movieCalendar;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime movieTime;
    private String movieTheater;
    private String movieImage;
    private String movieType;
    private int movieRating;
    private String movieGrade;
    private int movieSeatRemain;
    @Column
    private String movieSeat;

    public Long getTmdbMovieId() {
        return tmdbMovieId;
    }

    public void setTmdbMovieId(Long tmdbMovieId) {
        this.tmdbMovieId = tmdbMovieId;
    }
    public UUID getMovieId() {
        return movieId;
    }

    public void setMovieId(UUID movieId) {
        this.movieId = movieId;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public Date getMovieCalendar() {
        return movieCalendar;
    }

    public void setMovieCalendar(Date movieCalendar) {
        this.movieCalendar = movieCalendar;
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

    public String getMovieImage() {
        return movieImage;
    }

    public void setMovieImage(String movieImage) {
        this.movieImage = movieImage;
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

    public String getMovieGrade() {
        return movieGrade;
    }

    public void setMovieGrade(String movieGrade) {
        this.movieGrade = movieGrade;
    }

    public int getMovieSeatRemain() {
        return movieSeatRemain;
    }

    public void setMovieSeatRemain(int movieSeatRemain) {
        this.movieSeatRemain = movieSeatRemain;
    }
    public String getMovieSeat() {
        return movieSeat;
    }
    public void setMovieSeat(String movieSeat) {
        this.movieSeat = movieSeat;
    }

}
