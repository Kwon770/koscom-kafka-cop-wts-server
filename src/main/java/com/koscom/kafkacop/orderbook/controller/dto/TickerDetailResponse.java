package com.koscom.kafkacop.orderbook.controller.dto;

import java.time.LocalDateTime;

public record TickerDetailResponse(
	Integer tickerId,
	String tickerCode,
	String tickerName,
	Float tradePrice,
	Float changeRate,
	Float changePrice,
	Float accTradePrice,
	LocalDateTime localDateTime
) {
	public static TickerDetailResponse of(
		Integer tickerId,
		String tickerCode,
		String tickerName,
		Float tradePrice,
		Float changeRate,
		Float changePrice,
		Float accTradePrice,
		LocalDateTime localDateTime
	) {
		return new TickerDetailResponse(
			tickerId,
			tickerCode,
			tickerName,
			tradePrice,
			changeRate,
			changePrice,
			accTradePrice,
			localDateTime
		);
	}
}
