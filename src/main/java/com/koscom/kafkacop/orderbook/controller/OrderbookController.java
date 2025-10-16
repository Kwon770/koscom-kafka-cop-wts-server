package com.koscom.kafkacop.orderbook.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koscom.kafkacop.orderbook.controller.dto.OrderbooksPriceDetailResponse;
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
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, now, 5300000f, 100f),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, now, 5200000f, 200f),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, now, 5100000f, 300f),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, now, 5000000f, 400f),
				OrderbooksPriceDetailResponse.of(BidAskType.ASK, now, 4900000f, 500f),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, now, 4800000f, 600f),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, now, 4700000f, 700f),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, now, 4600000f, 800f),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, now, 4500000f, 900f),
				OrderbooksPriceDetailResponse.of(BidAskType.BID, now, 4400000f, 1000f)
			)
		);
	}
}
