package com.example.demo.interfaces.order;

import com.example.demo.application.order.OrderFacade;
import com.example.demo.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    public ApiResponse<Void> createOrder(@RequestBody OrderRequest.Order request) {
        orderFacade.order(request.toCriteria());
        return ApiResponse.success();
    }
}
