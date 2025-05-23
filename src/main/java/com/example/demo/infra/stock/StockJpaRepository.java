package com.example.demo.infra.stock;

import com.example.demo.domain.stock.Stock;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StockJpaRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "javax.persistence.lock.timeout", value = "1000"))
    @Query("SELECT s FROM Stock s WHERE s.productId = :productId")
    Optional<Stock> findWithPessimisticLock(@Param("productId") Long productId);

}
