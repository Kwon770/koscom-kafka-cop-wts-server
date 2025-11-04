package com.koscom.kafkacop.kafka.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Timestamp 변환 유틸리티
 * - 마이크로초/밀리초를 자동으로 감지하여 정밀도를 완전히 보존하면서 변환
 * - 모든 변환은 KST(Asia/Seoul) 시간대 기준
 * - LocalDateTime은 나노초까지 지원하므로 마이크로초 정밀도 완전 보존
 */
public class TimestampConverter {

	/** KST 시간대 (한국 표준시) */
	private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

	/**
	 * 타임스탬프를 KST(한국 표준시) LocalDateTime으로 변환
	 * - 마이크로초/밀리초 자동 감지
	 * - 마이크로초 정밀도 완전 보존 (나노초 단위까지)
	 * - 반드시 Asia/Seoul 시간대로 변환됨
	 *
	 * @param timestamp 타임스탬프 (밀리초 또는 마이크로초)
	 * @return LocalDateTime (KST 기준, 마이크로초 정밀도 보존)
	 */
	public static LocalDateTime toLocalDateTimeKst(long timestamp) {
		// 16자리 이상(10^15 이상)이면 마이크로초로 판단
		// 밀리초: 13자리 (예: 1609459200000 = 2021-01-01)
		// 마이크로초: 16자리 (예: 1609459200000000 = 2021-01-01)
		if (timestamp >= 1_000_000_000_000_000L) {
			// 마이크로초 단위 처리 (정밀도 완전 보존)
			// timestamp를 초 단위와 마이크로초 나머지로 분리
			long seconds = timestamp / 1_000_000;  // 초 단위
			long microRemainder = timestamp % 1_000_000;  // 마이크로초 나머지
			long nanos = microRemainder * 1_000;  // 마이크로초 → 나노초 변환

			return Instant.ofEpochSecond(seconds, nanos)
				.atZone(ZONE_KST)
				.toLocalDateTime();
		} else {
			// 밀리초 단위 처리
			return Instant.ofEpochMilli(timestamp)
				.atZone(ZONE_KST)
				.toLocalDateTime();
		}
	}

	/**
	 * 타임스탬프가 마이크로초 단위인지 확인
	 *
	 * @param timestamp 타임스탬프
	 * @return 마이크로초 단위면 true, 밀리초 단위면 false
	 */
	public static boolean isMicroseconds(long timestamp) {
		return timestamp >= 1_000_000_000_000_000L;
	}
}
