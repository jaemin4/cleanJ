package com.example.demo.interfaces.test;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test-endpoint")
    public String testEndpoint() {
        return "OK";
    }
}
