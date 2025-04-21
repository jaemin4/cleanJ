package com.example.demo.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    /**
     * 전달받은 ID 목록 중, 현재 판매 중인 상품만 필터링하여 반환
     */
    public ProductInfo.Products findSellingProductsByIds(ProductCommand.Products command) {
        List<Product> products = productRepository.findAllByIdIn(command.getProductIds());

        // 1. 조회된 상품이 없거나 개수가 일치하지 않으면 → 예외
        if (products.isEmpty() || products.size() != command.getProductIds().size()) {
            throw new RuntimeException("요청한 상품 중 존재하지 않는 상품이 포함되어 있습니다.");
        }

        // 2. 판매 중인 상품만 필터링
        List<ProductInfo.Product> sellingProducts = products.stream()
                .filter(product -> product.getSellStatus() == ProductSellingStatus.SELLING)
                .map(product -> ProductInfo.Product.of(
                        product.getId(),
                        product.getName(),
                        product.getPrice()
                ))
                .toList();

        // 3. 판매중 아닌 상품이 포함되어 있다면 → 예외
        if (sellingProducts.size() != products.size()) {
            throw new RuntimeException("요청한 상품 중 판매 중이지 않은 상품이 있습니다.");
        }

        return ProductInfo.Products.of(sellingProducts);
    }

    public ProductInfo.Product findById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new RuntimeException("해당 상품이 존재하지 않습니다.")
        );
        return ProductInfo.Product.of(product.getId(), product.getName(), product.getPrice());
    }

    public long calculateTotalPrice(ProductCommand.Products command) {
        List<Product> products = productRepository.findAllByIdIn(command.getProductIds());
        List<Long> invalidIds = products.stream()
                .filter(p -> p.getSellStatus() != ProductSellingStatus.SELLING)
                .map(Product::getId)
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "판매 중이지 않은 상품이 포함되어 있습니다. 상품 ID: " + invalidIds
            );
        }

        return products.stream()
                .mapToLong(Product::getPrice)
                .sum();
    }


}
