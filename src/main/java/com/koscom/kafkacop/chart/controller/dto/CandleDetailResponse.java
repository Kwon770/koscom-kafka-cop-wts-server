package com.koscom.kafkacop.chart.controller.dto;

import java.time.LocalDateTime;

public record CandleDetailResponse(
	LocalDateTime localDateTime,
	Float openPrice,
	Float highPrice,
	Float lowPrice,
	Float closePrice
) {
	public static CandleDetailResponse of(
		LocalDateTime localDateTime,
		Float openPrice,
		Float highPrice,
		Float lowPrice,
		Float closePrice
	) {
		return new CandleDetailResponse(
			localDateTime,
			openPrice,
			highPrice,
			lowPrice,
			closePrice
		);
	}
}
