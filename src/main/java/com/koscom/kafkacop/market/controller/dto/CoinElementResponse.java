package com.koscom.kafkacop.market.controller.dto;

import java.time.LocalDateTime;

public record CoinElementResponse(
	Integer tickerId,
	String tickerCode,
	String tickerName,
	Float tradePrice,
	Float changeRate,
	Float changePrice,
	Float accTradePrice,
	Float accTradePrice24h,
	LocalDateTime localDateTime
) {
	public static CoinElementResponse of(
		Integer tickerId,
		String tickerCode,
		String tickerName,
		Float tradePrice,
		Float changeRate,
		Float changePrice,
		Float accTradePrice,
		Float accTradePrice24h,
		LocalDateTime localDateTime
	) {
		return new CoinElementResponse(
			tickerId,
			tickerCode,
			tickerName,
			tradePrice,
			changeRate,
			changePrice,
			accTradePrice,
			accTradePrice24h,
			localDateTime
		);
	}
}
