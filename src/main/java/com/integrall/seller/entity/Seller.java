package com.integrall.seller.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

}