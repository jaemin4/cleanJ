package com.example.demo.unit;

import com.example.demo.domain.product.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProductTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("상품 이름이 비어있으면 예외 발생")
    void shouldThrowWhenNameIsBlank() {
        assertThatThrownBy(() -> Product.create(" ", 1000L, ProductSellingStatus.SELLING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 이름은 필수입니다.");
    }

    @Test
    @DisplayName("상품 가격이 0 이하이면 예외 발생")
    void shouldThrowWhenPriceIsInvalid() {
        assertThatThrownBy(() -> Product.create("책", 0L, ProductSellingStatus.SELLING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 가격은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("상품 판매 상태가 null이면 예외 발생")
    void shouldThrowWhenSellStatusIsNull() {
        assertThatThrownBy(() -> Product.create("책", 1000L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 판매 상태는 필수입니다.");
    }

    @Test
    @DisplayName("정상적으로 판매중인 상품만 조회된다")
    void findSellingProductsByIds_success() {
        // given
        Long id1 = 1L, id2 = 2L;
        Product p1 = Product.create("상품1", 1000, ProductSellingStatus.SELLING);
        Product p2 = Product.create("상품2", 2000, ProductSellingStatus.SELLING);
        setId(p1, id1);
        setId(p2, id2);

        List<Long> productIds = List.of(id1, id2);
        when(productRepository.findAllByIdIn(productIds)).thenReturn(List.of(p1, p2));

        ProductCommand.ProductIds command = ProductCommand.ProductIds.of(productIds);

        // when
        ProductInfo.Products result = productService.findSellingProductsByIds(command);

        // then
        assertThat(result.getProducts()).hasSize(2);
        assertThat(result.getProducts()).extracting("id").containsExactlyInAnyOrder(id1, id2);
    }

    @Test
    @DisplayName("존재하지 않는 상품이 포함된 경우 예외 발생")
    void findSellingProductsByIds_fail_not_found() {
        // given
        List<Long> ids = List.of(1L, 2L);
        when(productRepository.findAllByIdIn(ids)).thenReturn(List.of());

        // when & then
        ProductCommand.ProductIds command = ProductCommand.ProductIds.of(ids);
        assertThatThrownBy(() -> productService.findSellingProductsByIds(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("존재하지 않는 상품");
    }

    @Test
    @DisplayName("판매중이 아닌 상품이 포함된 경우 예외 발생")
    void findSellingProductsByIds_fail_not_selling() {
        // given
        Product p1 = Product.create("상품1", 1000, ProductSellingStatus.SELLING);
        Product p2 = Product.create("상품2", 2000, ProductSellingStatus.HOLD);
        setId(p1, 1L);
        setId(p2, 2L);

        when(productRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(p1, p2));

        // when & then
        ProductCommand.ProductIds command = ProductCommand.ProductIds.of(List.of(1L, 2L));
        assertThatThrownBy(() -> productService.findSellingProductsByIds(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("판매 중이지 않은 상품");
    }

    @Test
    @DisplayName("상품 ID로 단건 조회 성공")
    void findById_success() {
        // given
        Product product = Product.create("상품", 1000, ProductSellingStatus.SELLING);
        setId(product, 1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when
        ProductInfo.Product result = productService.findById(1L);

        // then
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo("상품");
        assertThat(result.getProductPrice()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("상품 ID로 조회 실패 시 예외 발생")
    void findById_fail() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("존재하지 않습니다");
    }

    @Test
    @DisplayName("판매 중인 상품들의 총 가격을 계산한다 - 수량 포함")
    void calculateTotalPrice_success() {
        // given
        Product p1 = Product.create("상품1", 1000L, ProductSellingStatus.SELLING);
        Product p2 = Product.create("상품2", 2000L, ProductSellingStatus.SELLING);
        setId(p1, 1L);
        setId(p2, 2L);

        List<ProductCommand.Products.OrderProduct> orderProducts = List.of(
                ProductCommand.Products.OrderProduct.of(1L, 2L), // 수량 2 → 2000원
                ProductCommand.Products.OrderProduct.of(2L, 1L)  // 수량 1 → 2000원
        );

        ProductCommand.Products command = ProductCommand.Products.of(orderProducts);

        when(productRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(p1, p2));

        // when
        long total = productService.calculateTotalPrice(command);

        // then
        assertThat(total).isEqualTo(4000L);
    }

    @Test
    @DisplayName("판매 중이지 않은 상품 포함 시 예외 발생")
    void calculateTotalPrice_fail_not_selling() {
        // given
        Product p1 = Product.create("상품1", 1000L, ProductSellingStatus.SELLING);
        Product p2 = Product.create("상품2", 2000L, ProductSellingStatus.STOP_SELLING);
        setId(p1, 1L);
        setId(p2, 2L);

        List<ProductCommand.Products.OrderProduct> orderProducts = List.of(
                ProductCommand.Products.OrderProduct.of(1L, 2L),
                ProductCommand.Products.OrderProduct.of(2L, 1L)
        );
        ProductCommand.Products command = ProductCommand.Products.of(orderProducts);

        when(productRepository.findAllByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(p1, p2));

        // when & then
        assertThatThrownBy(() -> productService.calculateTotalPrice(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("판매 중이지 않은 상품이 포함되어 있습니다");
    }


    // 리플렉션으로 private id 세팅
    private void setId(Product product, Long id) {
        try {
            Field field = Product.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(product, id);
        } catch (Exception e) {
            throw new RuntimeException("ID 설정 실패", e);
        }
    }



}
