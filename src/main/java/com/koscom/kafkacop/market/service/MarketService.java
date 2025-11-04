package com.koscom.kafkacop.market.service;

import com.koscom.kafkacop.market.controller.dto.CoinElementResponse;
import com.koscom.kafkacop.market.domain.Market;
import com.koscom.kafkacop.market.domain.Ticker;
import com.koscom.kafkacop.market.repository.MarketRepository;
import com.koscom.kafkacop.market.repository.TickerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketService {

	private final MarketRepository marketRepository;
	private final TickerRepository tickerRepository;

	/**
	 * 특정 거래소의 마켓 목록과 최신 Ticker 정보를 조회
	 *
	 * @param exchangeCode 거래소 코드 (예: UPBIT)
	 * @return 코인 목록 (마켓 정보 + Ticker 정보)
	 */
	public List<CoinElementResponse> getMarketList(String exchangeCode) {
		// 1. 거래소의 모든 마켓 조회
		List<Market> markets = marketRepository.findAllByExchangeCode(exchangeCode);

		if (markets.isEmpty()) {
			log.warn("No markets found for exchange: {}", exchangeCode);
			return List.of();
		}

		// 2. 마켓ID 리스트 추출
		List<Integer> marketIds = markets.stream()
				.map(Market::getMarketId)
				.collect(Collectors.toList());

		// 3. 각 마켓의 최신 Ticker 조회
		List<Ticker> tickers = tickerRepository.findLatestByMarketIds(marketIds);

		// 4. marketId -> Ticker 매핑
		Map<Integer, Ticker> tickerMap = tickers.stream()
				.collect(Collectors.toMap(Ticker::getMarketId, ticker -> ticker));

		// 5. Market + Ticker -> CoinElementResponse 변환
		return markets.stream()
				.map(market -> {
					Ticker ticker = tickerMap.get(market.getMarketId());
					if (ticker == null) {
						log.warn("No ticker found for market: {}", market.getMarketCode());
						// Ticker가 없는 경우 기본값으로 응답 생성
						return CoinElementResponse.of(
								market.getMarketId(),
								market.getMarketCode(),
								market.getMarketCode(), // tickerName이 없으므로 code로 대체
								ticker.getTradePrice().floatValue(),
								ticker.getSignedChangeRate().floatValue(),
								ticker.getSignedChangePrice().floatValue(),
								ticker.getAccTradePrice().floatValue(),
								ticker.getSourceCreatedAt()
						);
					}
					return convertToResponse(market, ticker);
				})
				.collect(Collectors.toList());
	}

	/**
	 * Market + Ticker를 CoinElementResponse로 변환
	 */
	private CoinElementResponse convertToResponse(Market market, Ticker ticker) {
		return CoinElementResponse.of(
				market.getMarketId(),
				market.getMarketCode(),
				market.getMarketCode(), // tickerName이 없으므로 code로 대체 (필요시 별도 필드 추가)
				ticker.getTradePrice().floatValue(),
				ticker.getSignedChangeRate().floatValue(),
				ticker.getSignedChangePrice().floatValue(),
				ticker.getAccTradePrice().floatValue(),
				ticker.getSourceCreatedAt()
		);
	}
}
