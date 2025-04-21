package com.example.demo.infra.stock;

import com.example.demo.domain.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockJpaRepository extends JpaRepository<Stock, Long> {
    Stock findByProductId(Long productId);
}
