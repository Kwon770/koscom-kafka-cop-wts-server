package com.koscom.kafkacop.orderbook.controller.dto;

import java.time.LocalDateTime;

import com.koscom.kafkacop.orderbook.domain.BidAskType;

public record OrderbooksPriceDetailResponse(
	BidAskType type,
	Float price,
	Float quantity,
	LocalDateTime localDateTime
) {
	public static OrderbooksPriceDetailResponse of(
		BidAskType type,
		Float price,
		Float quantity,
		LocalDateTime localDateTime
	) {
		return new OrderbooksPriceDetailResponse(
			type,
			price,
			quantity,
			localDateTime
		);
	}
}
