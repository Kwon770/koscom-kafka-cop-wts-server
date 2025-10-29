package com.koscom.kafkacop.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TickerExtendMessage(
        @JsonProperty("mkt_code")
        List<String> mktCode,

        @JsonProperty("exchange")
        String exchange,

        @JsonProperty("trade_price")
        long tradePrice,

        @JsonProperty("acc_trade_price")
        double accTradePrice,

        @JsonProperty("acc_trade_price_24h")
        double accTradePrice24h,

        @JsonProperty("timestamp")
        long timestamp
) {
}