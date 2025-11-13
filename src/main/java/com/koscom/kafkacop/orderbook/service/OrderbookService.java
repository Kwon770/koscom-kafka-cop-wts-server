package com.koscom.kafkacop.orderbook.service;

import com.koscom.kafkacop.market.domain.Market;
import com.koscom.kafkacop.market.domain.Ticker;
import com.koscom.kafkacop.market.repository.MarketRepository;
import com.koscom.kafkacop.market.repository.TickerRepository;
import com.koscom.kafkacop.orderbook.controller.dto.OrderbooksPriceDetailResponse;
import com.koscom.kafkacop.orderbook.controller.dto.TickerDetailResponse;
import com.koscom.kafkacop.orderbook.domain.BidAskType;
import com.koscom.kafkacop.orderbook.domain.Orderbook5;
import com.koscom.kafkacop.orderbook.repository.Orderbook5Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderbookService {

	private final Orderbook5Repository orderbook5Repository;
	private final TickerRepository tickerRepository;
	private final MarketRepository marketRepository;

	/**
	 * 5호가 조회
	 * 성능 최적화: Market 조회 → Orderbook5 조회 (2단계, JOIN 제거)
	 *
	 * @param exchangeCode 거래소 코드 (예: UPBIT)
	 * @param marketCode 마켓 코드 (예: KRW/BTC)
	 * @return 5호가 리스트 (ASK 5개 + BID 5개)
	 */
	public List<OrderbooksPriceDetailResponse> getOrderbooksPrice(String exchangeCode, String marketCode) {
		// 1단계: Market 조회 (ux_market 인덱스 활용)
		Market market = marketRepository.findByExchangeCodeAndMarketCode(exchangeCode, marketCode)
			.orElseThrow(() -> new IllegalArgumentException("Market not found for exchange: " + exchangeCode + ", market: " + marketCode));

		// 2단계: Orderbook5 조회 (PK 인덱스 활용, JOIN 없음)
		Orderbook5 orderbook = orderbook5Repository.findLatestByMarketId(market.getMarketId())
			.orElseThrow(() -> new IllegalArgumentException("Orderbook not found for exchange: " + exchangeCode + ", market: " + marketCode));

		List<OrderbooksPriceDetailResponse> result = new ArrayList<>();

		// ASK (매도호가) 5개 - 가격 높은 순
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.ASK,
			orderbook.getAskPrice5().floatValue(),
			orderbook.getAskQuantity5().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.ASK,
			orderbook.getAskPrice4().floatValue(),
			orderbook.getAskQuantity4().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.ASK,
			orderbook.getAskPrice3().floatValue(),
			orderbook.getAskQuantity3().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.ASK,
			orderbook.getAskPrice2().floatValue(),
			orderbook.getAskQuantity2().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.ASK,
			orderbook.getAskPrice1().floatValue(),
			orderbook.getAskQuantity1().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));

		// BID (매수호가) 5개 - 가격 낮은 순
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.BID,
			orderbook.getBidPrice1().floatValue(),
			orderbook.getBidQuantity1().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.BID,
			orderbook.getBidPrice2().floatValue(),
			orderbook.getBidQuantity2().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.BID,
			orderbook.getBidPrice3().floatValue(),
			orderbook.getBidQuantity3().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.BID,
			orderbook.getBidPrice4().floatValue(),
			orderbook.getBidQuantity4().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));
		result.add(OrderbooksPriceDetailResponse.of(
			BidAskType.BID,
			orderbook.getBidPrice5().floatValue(),
			orderbook.getBidQuantity5().floatValue(),
			orderbook.getId().getOrderbookDateTime()
		));

		return result;
	}

	/**
	 * 현재가 조회
	 *
	 * @param exchangeCode 거래소 코드 (예: UPBIT)
	 * @param marketCode 마켓 코드 (예: KRW/BTC)
	 * @return 현재가 정보
	 */
	public TickerDetailResponse getCurrentPrice(String exchangeCode, String marketCode) {
		Market market = marketRepository.findByExchangeCodeAndMarketCode(exchangeCode, marketCode)
			.orElseThrow(() -> new IllegalArgumentException("Market not found for exchange: " + exchangeCode + ", market: " + marketCode));

		Ticker ticker = tickerRepository.findLatestByMarketId(market.getMarketId())
			.orElseThrow(() -> new IllegalArgumentException("Ticker not found for market: " + marketCode));

		return TickerDetailResponse.of(
			market.getMarketId(),
			marketCode,
			"비트코인", // static name
			ticker.getTradePrice().floatValue(),
			ticker.getSignedChangeRate().floatValue(),
			ticker.getSignedChangePrice().floatValue(),
			ticker.getAccTradePrice().floatValue(),
			ticker.getSourceCreatedAt()
		);
	}
}
