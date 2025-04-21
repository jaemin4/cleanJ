package com.example.demo.infra.order;

import com.example.demo.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    Order findByUserId(Long userId);

}
