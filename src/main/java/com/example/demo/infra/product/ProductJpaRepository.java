package com.example.demo.infra.product;

import com.example.demo.domain.product.Product;
import com.example.demo.domain.product.ProductSellingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;


public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    List<Product> findAllBySellStatusIn(Collection<ProductSellingStatus> sellStatuses);

    List<Product> findAllByIdIn(Collection<Long> ids);


}
