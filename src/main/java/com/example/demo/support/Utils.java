package com.example.demo.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    //TODO 객체 → JSON 문자열
    public static String toJson(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON 직렬화 실패: {}", e.getMessage());
            return "json 직렬화 실패";
        }
    }

    //TODO  JSON 문자열 → 객체
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("[Utils] JSON 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 소수점 이하를 반올림하여 long 타입으로 변환합니다.
     *
     * @param price 원래 금액 (정수 또는 소수)
     * @return 반올림된 long 값
     */
    public static long toRoundedLong(double price) {
        return BigDecimal.valueOf(price)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

}
