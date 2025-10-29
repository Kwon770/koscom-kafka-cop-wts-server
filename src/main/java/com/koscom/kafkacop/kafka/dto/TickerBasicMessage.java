package com.koscom.kafkacop.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TickerBasicMessage(
        @JsonProperty("mkt_code")
        List<String> mktCode,

        @JsonProperty("exchange")
        String exchange,

        @JsonProperty("trade_price")
        long tradePrice,

        @JsonProperty("signed_change_price")
        long signedChangePrice,

        @JsonProperty("signed_change_rate")
        double signedChangeRate,

        @JsonProperty("timestamp")
        long timestamp
) {
}
