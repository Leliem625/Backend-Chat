package com.example.demo.repository;

import com.example.demo.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByRefreshToken(String refreshToken);

    Optional<Session> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByRefreshToken(String refreshToken);
}
