package com.example.demo.application.order;

import com.example.demo.domain.coupon.CouponCommand;
import com.example.demo.domain.order.OrderCommand;
import com.example.demo.domain.product.ProductCommand;
import com.example.demo.domain.stock.StockCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderCriteria {

    @Getter
    public static class Order{
        private final Long userId;
        private final Long couponId;
        private final List<OrderProduct> items;

        private Order(Long userId, Long couponId, List<OrderProduct> items) {
            this.userId = userId;
            this.couponId = couponId;
            this.items = items;
        }

        public StockCommand.deductStock toDeductStockCommand() {
            List<StockCommand.OrderProduct> stockProducts =
                    items.stream()
                            .map(i -> StockCommand.OrderProduct.of(
                                    i.getProductId(),
                                    i.getQuantity()
                            ))
                            .toList();
            return StockCommand.deductStock.of(stockProducts);
        }

        public CouponCommand.Use toCouponUseCommand() {
            return CouponCommand.Use.of( userId,couponId);
        }

        public ProductCommand.Products toProductsCommand() {
            List<Long> productIds = items.stream()
                    .map(OrderProduct::getProductId)
                    .toList();

            return ProductCommand.Products.of(productIds);
        }

        public CouponCommand.GetDiscountRate toGetDiscountRateCommand() {
            return CouponCommand.GetDiscountRate.of(couponId);
        }

        public static Order of(Long userId, Long couponId, List<OrderProduct> items) {
            return new Order(userId, couponId,items);
        }

        public OrderCommand.CreateOrder toCreateOrderCommand(double discountRate, long productTotalAmount) {
            List<OrderCommand.OrderProduct> products =
                    items.stream()
                            .map(i -> OrderCommand.OrderProduct.of(
                                    i.getProductId(),
                                    i.getQuantity()
                            ))
                            .toList();

            return OrderCommand.CreateOrder.of(userId,discountRate,productTotalAmount,products);
        }
    }

    @Getter
    public static class OrderProduct{
        private final Long productId;
        private final Long quantity;

        private OrderProduct(Long productId, Long quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public static OrderProduct of(Long productId, Long quantity) {
            return new OrderProduct(productId,quantity);
        }


    }

}
