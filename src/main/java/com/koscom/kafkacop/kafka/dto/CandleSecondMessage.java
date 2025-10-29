package com.koscom.kafkacop.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CandleSecondMessage(
        @JsonProperty("mkt_code")
        List<String> mktCode,

        @JsonProperty("exchange")
        String exchange,

        @JsonProperty("candle_date_time_utc")
        String candleDateTimeUtc,

        @JsonProperty("candle_date_time_kst")
        String candleDateTimeKst,

        @JsonProperty("opening_price")
        long openingPrice,

        @JsonProperty("high_price")
        long highPrice,

        @JsonProperty("low_price")
        long lowPrice,

        @JsonProperty("trade_price")
        long tradePrice,

        @JsonProperty("candle_acc_trade_volume")
        double candleAccTradeVolume,

        @JsonProperty("candle_acc_trade_price")
        double candleAccTradePrice,

        @JsonProperty("timestamp")
        long timestamp
) {
}