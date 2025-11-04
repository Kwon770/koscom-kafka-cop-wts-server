package com.koscom.kafkacop.market.repository;

import com.koscom.kafkacop.market.domain.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

	/**
	 * 거래소 코드와 마켓 코드로 Market 조회
	 * JOIN FETCH로 Exchange를 함께 조회하여 N+1 문제 방지
	 *
	 * @param exchangeCode 거래소 코드 (예: UPBIT)
	 * @param marketCode 마켓 코드 (예: KRW/BTC)
	 * @return Market 엔티티
	 */
	@Query("SELECT m FROM Market m JOIN FETCH m.exchange WHERE m.exchange.exchangeCode = :exchangeCode AND m.marketCode = :marketCode")
	Optional<Market> findByExchangeCodeAndMarketCode(
		@Param("exchangeCode") String exchangeCode,
		@Param("marketCode") String marketCode
	);

	/**
	 * 마켓 코드로 Market 조회 (Deprecated - exchange 파라미터를 함께 사용해야 함)
	 * JOIN FETCH로 Exchange를 함께 조회하여 N+1 문제 방지
	 *
	 * @param marketCode 마켓 코드 (예: KRW/BTC)
	 * @return Market 엔티티
	 */
	@Deprecated
	@Query("SELECT m FROM Market m JOIN FETCH m.exchange WHERE m.marketCode = :marketCode")
	Optional<Market> findByMarketCode(@Param("marketCode") String marketCode);

	/**
	 * 거래소 코드로 모든 Market 조회
	 * JOIN FETCH로 Exchange를 함께 조회하여 N+1 문제 방지
	 *
	 * @param exchangeCode 거래소 코드 (예: UPBIT)
	 * @return Market 엔티티 리스트
	 */
	@Query("SELECT m FROM Market m JOIN FETCH m.exchange WHERE m.exchange.exchangeCode = :exchangeCode")
	List<Market> findAllByExchangeCode(@Param("exchangeCode") String exchangeCode);
}
