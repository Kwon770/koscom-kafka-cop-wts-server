package com.koscom.kafkacop.chart.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koscom.kafkacop.chart.controller.dto.CandleDetailResponse;
import com.koscom.kafkacop.util.ArrayResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/charts")
public class ChartController {

	@Operation(summary = "캔들 차트 조회 (BTCKRW, 초봉 고정)")
	@GetMapping("/candle")
	ArrayResponse<CandleDetailResponse> getCandleChart() {
		return ArrayResponse.of(
			List.of(
				CandleDetailResponse.of(LocalDateTime.now(), 5000000f, 5500000f, 4500000f, 5200000f)
			)
		);
	}
}
