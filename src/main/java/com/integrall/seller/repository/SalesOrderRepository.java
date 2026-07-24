package com.integrall.seller.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.integrall.seller.entity.SalesOrder;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {
}