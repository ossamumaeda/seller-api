package com.integrall.seller.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.integrall.seller.entity.Seller;

public interface SellerRepository extends JpaRepository<Seller, UUID> {
}