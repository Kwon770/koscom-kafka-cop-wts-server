package com.koscom.kafkacop.orderbook.controller.dto;

import java.time.LocalDateTime;

import com.koscom.kafkacop.orderbook.domain.BidAskType;

public record OrderbooksPriceDetailResponse(
	BidAskType type,
	LocalDateTime localDateTime,
	Float price,
	Float quantity
) {
	public static OrderbooksPriceDetailResponse of(
		BidAskType type,
		LocalDateTime localDateTime,
		Float price,
		Float quantity
	) {
		return new OrderbooksPriceDetailResponse(
			type,
			localDateTime,
			price,
			quantity
		);
	}
}
