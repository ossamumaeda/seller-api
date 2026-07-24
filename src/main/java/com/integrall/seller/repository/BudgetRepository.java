package com.integrall.seller.repository;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.integrall.seller.entity.Budget;

import jakarta.persistence.LockModeType;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findBySellerIdAndCompetence(
            UUID sellerId,
            LocalDate competence
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b
        from Budget b
        where b.seller.id = :sellerId
          and b.competence = :competence
        """)
    Optional<Budget> findForUpdate(
            @Param("sellerId") UUID sellerId,
            @Param("competence") LocalDate competence
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b
        from Budget b
        where b.id = :id
        """)
    Optional<Budget> findByIdForUpdate(@Param("id") UUID id);

    List<Budget> findAllByOrderByCompetenceDescSellerNameAsc();

}