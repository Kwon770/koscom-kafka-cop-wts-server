package com.koscom.kafkacop.orderbook.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koscom.kafkacop.orderbook.controller.dto.OrderbooksPriceDetailResponse;
import com.koscom.kafkacop.orderbook.controller.dto.TickerDetailResponse;
import com.koscom.kafkacop.orderbook.domain.BidAskType;
import com.koscom.kafkacop.orderbook.service.OrderbookService;
import com.koscom.kafkacop.util.ArrayResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orderbooks")
public class OrderbookController {

	private final OrderbookService orderbookService;

	@Operation(summary = "5 호가 조회")
	@GetMapping("/orderbooks-price")
	ArrayResponse<OrderbooksPriceDetailResponse> getOrderbooksPrice(
		@Parameter(description = "거래소 코드 (예: UPBIT)", example = "UPBIT", required = true)
		@RequestParam String exchange,

		@Parameter(description = "마켓 코드 (예: KRW/BTC)", example = "KRW/BTC")
		@RequestParam(defaultValue = "KRW/BTC") String marketCode
	) {
		List<OrderbooksPriceDetailResponse> orderbooks = orderbookService.getOrderbooksPrice(exchange, marketCode);
		return ArrayResponse.of(orderbooks);
	}

	@Operation(summary = "현재가 조회")
	@GetMapping("/ticker")
	TickerDetailResponse getCurrentPrice(
		@Parameter(description = "거래소 코드 (예: UPBIT)", example = "UPBIT", required = true)
		@RequestParam String exchange,

		@Parameter(description = "마켓 코드 (예: KRW/BTC)", example = "KRW/BTC")
		@RequestParam String marketCode
	) {
		return orderbookService.getCurrentPrice(exchange, marketCode);
	}

	@Operation(summary = "체결내역 조회 (미구현)")
	@GetMapping("/trades")
	ArrayResponse<OrderbooksPriceDetailResponse> getTrades() {
		LocalDateTime now = LocalDateTime.now();

		return ArrayResponse.of(
			List.of(
				OrderbooksPriceDetailResponse.of(BidAskType.BID, 5000000f, 0.1f, now.minusSeconds(5)),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, 5100000f, 0.2f, now.minusSeconds(4)),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, 4900000f, 0.15f, now.minusSeconds(3)),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, 5200000f, 0.25f, now.minusSeconds(2)),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, 4800000f, 0.3f, now.minusSeconds(1))
			)
		);
	}
}
