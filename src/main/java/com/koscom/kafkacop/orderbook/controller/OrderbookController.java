package com.koscom.kafkacop.orderbook.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koscom.kafkacop.orderbook.controller.dto.OrderbooksPriceDetailResponse;
import com.koscom.kafkacop.orderbook.controller.dto.TickerDetailResponse;
import com.koscom.kafkacop.orderbook.domain.BidAskType;
import com.koscom.kafkacop.util.ArrayResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/orderbooks")
public class OrderbookController {

	@Operation(summary = "5 호가 조회 (BTCKRW 고정)")
	@GetMapping("/orderbooks-price")
	ArrayResponse<OrderbooksPriceDetailResponse> getOrderbooksPrice() {
		LocalDateTime now = LocalDateTime.now();

		return ArrayResponse.of(
			List.of(
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, 5300000f, 100f, now),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, 5200000f, 200f, now),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, 5100000f, 300f, now),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, 5000000f, 400f, now),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, 4900000f, 500f, now),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, 4800000f, 600f, now),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, 4700000f, 700f, now),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, 4600000f, 800f, now),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, 4500000f, 900f, now),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, 4400000f, 1000f, now)
			)
		);
	}

	@Operation(summary = "현재가 조회 (BTCKRW 고정)")
	@GetMapping("/ticker")
	TickerDetailResponse getCurrentPrice() {
		return TickerDetailResponse.of(
			1,
			"BTCKRW",
			"비트코인",
			5000000f,
			2f,
			100000f,
			1500000000f,
			LocalDateTime.now()
		);
	}

	@Operation(summary = "체결내역 조회 (BTCKRW 고정)")
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
