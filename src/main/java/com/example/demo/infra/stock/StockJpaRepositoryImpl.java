//package com.example.demo.infra.stock;
//
//import clean.ecd.domain.stock.Stock;
//import clean.ecd.domain.stock.StockRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.stereotype.Repository;
//
//@Repository
//@RequiredArgsConstructor
//public class StockJpaRepositoryImpl implements StockRepository {
//
//    private final  @Lazy StockJpaRepository stockJpaRepository;
//
//    @Override
//    public Stock findByProductId(Long productId) {
//        return stockJpaRepository.findByProductId(productId);
//    }
//}  // todo 순환 잠조 문제가 발생하였습니다
