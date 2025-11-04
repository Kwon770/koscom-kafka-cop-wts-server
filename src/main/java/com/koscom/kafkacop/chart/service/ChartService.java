package com.koscom.kafkacop.chart.service;

import com.koscom.kafkacop.chart.controller.dto.CandleDetailResponse;
import com.koscom.kafkacop.chart.domain.Candle;
import com.koscom.kafkacop.chart.domain.CandleType;
import com.koscom.kafkacop.chart.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChartService {

    private final CandleRepository candleRepository;

    /**
     * 캔들 차트 데이터 조회
     *
     * @param exchangeCode 거래소 코드 (예: UPBIT)
     * @param code 마켓 코드 (예: KRW/BTC)
     * @param type 캔들 타입 (예: ONE_SECOND, ONE_MINUTE 등)
     * @param from 시작 시간 (선택)
     * @param to 종료 시간 (선택)
     * @return 캔들 차트 응답 리스트
     */
    public List<CandleDetailResponse> getCandleChart(
            String exchangeCode,
            String code,
            CandleType type,
            LocalDateTime from,
            LocalDateTime to
    ) {
        // 기본값 설정
        CandleType candleType = type != null ? type : CandleType.ONE_SECOND;
        LocalDateTime fromTime = from;
        LocalDateTime toTime = to;

        // from, to가 모두 없으면 최근 24시간을 기본값으로 설정
        if (fromTime == null && toTime == null) {
            toTime = LocalDateTime.now();
            fromTime = toTime.minusDays(1);
        }
        // from만 없으면 to 기준 24시간 전을 from으로 설정
        else if (fromTime == null) {
            fromTime = toTime.minusDays(1);
        }
        // to만 없으면 from 기준 24시간 후를 to로 설정
        else if (toTime == null) {
            toTime = fromTime.plusDays(1);
        }

        // 시간 범위로 조회
        List<Candle> candles = candleRepository.findByExchangeCodeAndCodeAndTypeAndDateTimeBetween(
                exchangeCode, code, candleType, fromTime, toTime
        );

		System.out.println(candles.size());

        // Entity -> DTO 변환
        return candles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Candle Entity를 CandleDetailResponse DTO로 변환
     */
    private CandleDetailResponse convertToResponse(Candle candle) {
        return CandleDetailResponse.of(
                candle.getId().getCandleDateTime(),
                candle.getOpeningPrice().floatValue(),
                candle.getHighPrice().floatValue(),
                candle.getLowPrice().floatValue(),
                candle.getTradePrice().floatValue()
        );
    }
}
