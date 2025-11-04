package com.koscom.kafkacop.chart.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koscom.kafkacop.chart.controller.dto.CandleDetailResponse;
import com.koscom.kafkacop.chart.domain.CandleType;
import com.koscom.kafkacop.chart.service.ChartService;
import com.koscom.kafkacop.util.ArrayResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/charts")
public class ChartController {

	private final ChartService chartService;

	@Operation(summary = "캔들 차트 조회", description = "거래소와 마켓 코드, 캔들 타입으로 차트 데이터를 조회합니다. 확대, 축소, 이동이 가능합니다.")
	@GetMapping("/candle")
	ArrayResponse<CandleDetailResponse> getCandleChart(
			@Parameter(description = "거래소 코드 (예: UPBIT)", example = "UPBIT", required = true)
			@RequestParam
			String exchange,

			@Parameter(description = "마켓 코드 (예: KRW/BTC)", example = "KRW/BTC", required = true)
			@RequestParam
			String code,

			@Parameter(description = "캔들 타입 (1s, 1m, 3m, 5m, 10m, 15m, 30m, 1h, 4h, 1d, 1w, 1M)", example = "1s")
			@RequestParam(required = false, defaultValue = "1s")
			String type,

			@Parameter(description = "조회 시작 시간 (ISO-8601 형식)", example = "2024-11-04T00:00:00")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime from,

			@Parameter(description = "조회 종료 시간 (ISO-8601 형식)", example = "2024-11-04T23:59:59")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
			LocalDateTime to
	) {
		// 캔들 타입 변환
		CandleType candleType = CandleType.fromCode(type);

		// 서비스 호출
		List<CandleDetailResponse> candles = chartService.getCandleChart(
				exchange, code, candleType, from, to
		);

		return ArrayResponse.of(candles);
	}
}
