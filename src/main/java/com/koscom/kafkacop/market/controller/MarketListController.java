package com.koscom.kafkacop.market.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koscom.kafkacop.market.controller.dto.CoinElementResponse;
import com.koscom.kafkacop.market.service.MarketService;
import com.koscom.kafkacop.util.ArrayResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/markets")
public class MarketListController {

	private final MarketService marketService;

	@Operation(summary = "코인 목록 조회", description = "특정 거래소의 모든 마켓 목록과 최신 Ticker 정보를 조회합니다.")
	@GetMapping("/list")
	ArrayResponse<CoinElementResponse> getMarketList(
			@Parameter(description = "거래소 코드 (예: UPBIT)", example = "UPBIT", required = true)
			@RequestParam String exchange
	) {
		List<CoinElementResponse> markets = marketService.getMarketList(exchange);
		return ArrayResponse.of(markets);
	}
}
