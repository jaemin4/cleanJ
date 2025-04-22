package com.example.demo.unit;

import com.example.demo.domain.coupon.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CouponTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @InjectMocks
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("정상적으로 쿠폰을 발급한다")
    @Test
    void issueCoupon_success() {
        // given
        Long couponId = 1L;
        Long userId = 10L;
        Coupon coupon = Coupon.create("할인쿠폰", 20, 5L);
        CouponCommand.Issue command = CouponCommand.Issue.of(couponId, userId);
        when(couponRepository.findById(anyLong())).thenReturn(Optional.of(coupon));
        when(userCouponRepository.findByCouponIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        // when
        couponService.issue(command);

        // then
        verify(userCouponRepository, times(1)).save(any(UserCoupon.class));
        assertThat(coupon.getQuantity()).isEqualTo(4); // 수량 1 감소
    }


    @DisplayName("이미 발급된 쿠폰은 중복 발급할 수 없다")
    @Test
    void issueCoupon_duplicate_fail() {
        // given
        Long couponId = 1L;
        Long userId = 10L;
        Coupon coupon = Coupon.create("할인쿠폰", 20, 5L);
        CouponCommand.Issue command = CouponCommand.Issue.of(couponId, userId);

        when(userCouponRepository.findByCouponIdAndUserId(couponId, userId))
                .thenReturn(Optional.empty())  // 첫 번째
                .thenReturn(Optional.of(mock(UserCoupon.class)));

        when(couponRepository.findById(couponId))
                .thenReturn(Optional.of(coupon))
                .thenReturn(Optional.of(coupon));

        couponService.issue(command);
        assertThat(coupon.getQuantity()).isEqualTo(4);

        assertThatThrownBy(() -> couponService.issue(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("중복 발급이 불가능");
    }



    @DisplayName("수량이 0인 쿠폰은 발급할 수 없다")
    @Test
    void issueCoupon_quantity_fail() {
        // given
        Long couponId = 1L;
        Long userId = 10L;
        CouponCommand.Issue command = CouponCommand.Issue.of(couponId, userId);
        Coupon coupon = Coupon.create("할인쿠폰", 10, 0L);

        when(userCouponRepository.findByCouponIdAndUserId(couponId, userId)).thenReturn(Optional.empty());
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.issue(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("쿠폰 수량이 부족");
    }

    @DisplayName("정상적으로 쿠폰을 사용할 수 있다")
    @Test
    void useCoupon_success() {
        // given
        Long userId = 10L;
        Long userCouponId = 100L;
        CouponCommand.Use command = CouponCommand.Use.of(userCouponId, userId);
        UserCoupon userCoupon = UserCoupon.issue(1L, userId);

        when(userCouponRepository.findByCouponId(userCouponId)).thenReturn(Optional.of(userCoupon));

        // when
        couponService.use(command);

        // then
        assertThat(userCoupon.isUsed()).isTrue();
    }

    @DisplayName("다른 유저의 쿠폰을 사용하려 하면 예외 발생")
    @Test
    void useCoupon_invalidUser_fail() {
        // given
        Long userCouponId = 100L;
        Long userId = 10L;
        Long otherUserId = 99L;
        CouponCommand.Use command =CouponCommand.Use.of(userCouponId, userId);
        UserCoupon userCoupon = UserCoupon.issue(1L, otherUserId);

        when(userCouponRepository.findByCouponId(userCouponId)).thenReturn(Optional.of(userCoupon));

        // when & then
        assertThatThrownBy(() -> couponService.use(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 유저의 쿠폰이 아닙니다");
    }

    @DisplayName("정상적으로 쿠폰의 할인율을 조회한다")
    @Test
    void getDiscountRate_success() {
        // given
        Long couponId = 1L;
        CouponCommand.GetDiscountRate command = CouponCommand.GetDiscountRate.of(couponId);
        Coupon coupon = Coupon.create("쿠폰", 25, 5L);

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // when
        double rate = couponService.getDiscountRate(command);

        // then
        assertThat(rate).isEqualTo(25);
    }
}
