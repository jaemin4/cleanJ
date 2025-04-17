package com.example.demo.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long productId);
    List<Product> findAllByIdIn(List<Long> productIds);
}
