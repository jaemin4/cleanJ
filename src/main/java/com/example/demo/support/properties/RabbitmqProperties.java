package com.example.demo.support.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitmqProperties {
    private String host;
    private int port;
    private String username;
    private String password;
}
