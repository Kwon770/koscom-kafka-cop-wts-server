package com.koscom.kafkacop.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isBlank()) return null;

        // "T24:00:00"을 다음날 "T00:00:00"으로 변환
        if (value.matches(".*T24:00:00$")) {
            String datePart = value.substring(0, 10);
            LocalDateTime nextDay = LocalDateTime.parse(datePart + "T00:00:00").plusDays(1);
            return nextDay;
        }
        // 일반적으로 파싱
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null; // 필요시 다른 처리
        }
    }
}
