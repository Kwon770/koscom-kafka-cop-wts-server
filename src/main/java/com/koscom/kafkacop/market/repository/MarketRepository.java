package com.koscom.kafkacop.market.repository;

import com.koscom.kafkacop.market.domain.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MarketRepository extends JpaRepository<Market, Integer> {

	/**
	 * 거래소 코드와 마켓 코드 리스트로 Market 목록 조회 (배치 조회 최적화)
	 * JOIN FETCH로 Exchange를 함께 조회하여 N+1 문제 방지
	 *
	 * @param exchangeCode 거래소 코드 (예: UPBIT)
	 * @param marketCodes 마켓 코드 리스트 (예: [KRW/BTC, KRW/ETH])
	 * @return Market 엔티티 리스트
	 */
	@Query("SELECT m FROM Market m JOIN FETCH m.exchange WHERE m.exchange.exchangeCode = :exchangeCode AND m.marketCode IN :marketCodes")
	List<Market> findAllByExchange_ExchangeCodeAndMarketCodeIn(
		@Param("exchangeCode") String exchangeCode,
		@Param("marketCodes") List<String> marketCodes
	);
}
