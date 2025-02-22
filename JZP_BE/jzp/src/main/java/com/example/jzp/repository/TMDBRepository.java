package com.example.jzp.repository;

import com.example.jzp.model.TMDB;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TMDBRepository extends JpaRepository<TMDB, Long> {
    TMDB findByTmdbMovieId(Long tmdbMovieId);
}