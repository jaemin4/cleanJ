package com.example.demo.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductInfo.Products findSellingProductsByIds(ProductCommand.ProductIds command) {
        List<Product> products = productRepository.findAllByIdIn(command.getProductIds());

        if (products.isEmpty() || products.size() != command.getProductIds().size()) {
            throw new RuntimeException("요청한 상품 중 존재하지 않는 상품이 포함되어 있습니다.");
        }

        List<ProductInfo.Product> sellingProducts = products.stream()
                .filter(product -> product.getSellStatus() == ProductSellingStatus.SELLING)
                .map(product -> ProductInfo.Product.of(
                        product.getId(),
                        product.getName(),
                        product.getPrice()
                ))
                .toList();

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
        List<Long> productIds = command.getProducts().stream()
                .map(ProductCommand.Products.OrderProduct::getProductId)
                .toList();

        List<Product> products = productRepository.findAllByIdIn(productIds);

        if (products.size() != productIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 상품이 포함되어 있습니다.");
        }

        List<Long> invalidIds = products.stream()
                .filter(p -> p.getSellStatus() != ProductSellingStatus.SELLING)
                .map(Product::getId)
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("판매 중이지 않은 상품이 포함되어 있습니다. 상품 ID: " + invalidIds);
        }

        return command.getProducts().stream()
                .mapToLong(orderProduct -> {
                    Product product = products.stream()
                            .filter(p -> p.getId().equals(orderProduct.getProductId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("상품 정보 누락: " + orderProduct.getProductId()));
                    return product.getPrice() * orderProduct.getQuantity();
                })
                .sum();
    }


}
