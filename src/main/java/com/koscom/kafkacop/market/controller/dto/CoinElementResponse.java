package com.koscom.kafkacop.market.controller.dto;

import java.time.LocalDateTime;

public record CoinElementResponse(
	Integer id,
	String code,
	String name,
	Float tradePrice,
	Float changeRate,
	Float changePrice,
	Float accTradePrice,
	LocalDateTime localDateTime
) {
	public static CoinElementResponse of(
		Integer id,
		String code,
		String name,
		Float tradePrice,
		Float changeRate,
		Float changePrice,
		Float accTradePrice,
		LocalDateTime localDateTime
	) {
		return new CoinElementResponse(
			id,
			code,
			name,
			tradePrice,
			changeRate,
			changePrice,
			accTradePrice,
			localDateTime
		);
	}
}
