package com.hyperativa.cardapi.repository;

import com.hyperativa.cardapi.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardHash(String cardHash);
    boolean existsByCardHash(String cardHash);
}
