package com.koscom.kafkacop.market.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koscom.kafkacop.market.controller.dto.CoinElementResponse;
import com.koscom.kafkacop.util.ArrayResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/markets")
public class MarketListController {

	@Operation(summary = "코인 목록 조회")
	@GetMapping("/list")
	ArrayResponse<CoinElementResponse> getMarketList() {
		LocalDateTime now = LocalDateTime.now();

		return ArrayResponse.of(
			List.of(
				CoinElementResponse.of(1, "BTC", "비트코인", 50000000f, 2f, 1000000f, 1500000000f, now),
				CoinElementResponse.of(2, "ETH", "이더리움", 3000000f, -1f, -30000f, 800000000f, now),
				CoinElementResponse.of(3, "XRP", "리플", 800f, 0f, 0f, 200000000f, now),
				CoinElementResponse.of(4, "ADA", "에이다", 1200f, 3f, 36f, 100000000f, now),
				CoinElementResponse.of(5, "DOGE", "도지코인", 250f, -2f, -5f, 50000000f, now)
			)
		);
	}
}
