package com.koscom.kafkacop.chart.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * CandleType Enum의 code 값을 DB에 저장하고 조회하는 Converter
 */
@Converter(autoApply = true)
public class CandleTypeConverter implements AttributeConverter<CandleType, String> {

    @Override
    public String convertToDatabaseColumn(CandleType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public CandleType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return CandleType.fromCode(dbData);
    }
}
