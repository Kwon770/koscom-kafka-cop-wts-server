package com.koscom.kafkacop.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Orderbook5Message(
        @JsonProperty("mkt_code")
        List<String> mktCode,

        @JsonProperty("exchange")
        String exchange,

        @JsonProperty("timestamp")
        long timestamp,

        @JsonProperty("total_ask_size")
        double totalAskSize,

        @JsonProperty("total_bid_size")
        double totalBidSize,

        @JsonProperty("OrderbookUnits")
        List<OrderbookUnit> orderbookUnits
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderbookUnit(
            @JsonProperty("ask_price")
            double askPrice,

            @JsonProperty("bid_price")
            double bidPrice,

            @JsonProperty("ask_size")
            double askSize,

            @JsonProperty("bid_size")
            double bidSize
    ) {
    }
}