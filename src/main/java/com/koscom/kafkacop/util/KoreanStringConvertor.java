package com.koscom.kafkacop.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class KoreanStringConvertor {
    public static String toPriceString(double price) {
        long value = (long) price; // 소수점 버림

        if (value == 0) {
            return "0원";
        }

        StringBuilder sb = new StringBuilder();

        long eok = value / 100_000_000;
        long man = (value % 100_000_000) / 10_000;
        long chun = (value % 10_000) / 1_000;
        long baek = (value % 1_000) / 100;

        if (eok > 0) {
            sb.append(eok).append("억");
        }
        if (man > 0) {
            sb.append(man).append("만");
        }
        if (chun > 0) {
            sb.append(chun).append("천");
        }
        if (baek > 0) {
            sb.append(baek).append("백");
        }
        return sb.toString();
    }

    public static String toCompactAddress(String address) {
        // 공백, 불필요한 단어 전부 제거
        address = address.replace(" ", "");
        address = address.replace("번지", "");

        // 주소에서 서울 관련 부분 제거
        String compactAddress = address.replace("서울특별시", "").replace("서울시", "").replace("서울", "").trim();

        // 자치구 ~ 지번까지만 남기고, 나머지 부분 제거
        Pattern pattern = Pattern.compile("^(.*?)(\\d+-\\d+|\\d+)");
        Matcher matcher = pattern.matcher(compactAddress);
        if (matcher.find()) {
            compactAddress = matcher.group(1) + matcher.group(2);
        }

        return compactAddress;
    }

    public static String toNoWhiteSpace(String str) {
        // 문자열의 모든 공백 제거
        return str.replaceAll("\\s+", "");
    }

    public static String toCompactUrl(String url) {
        url = url.replace("https://", "");
        url = url.replace("http://", "");

        // 마지막이 "/"로 끝나면 제거
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    public static String extractHouseName(String str) {
        String name = toNoWhiteSpace(str);

        // "□ 주택명 : 은평뉴타운 2단지" 등의 주택명 유형의 본문에서 주택명만 분리하는 정규식
        Pattern pattern1 = Pattern.compile("^[■□]?주택명[\\s:：·-]*([^()]+?)(?:\\s*\\((.+)\\))?$");
        Matcher matcher1 = pattern1.matcher(name);
        if (matcher1.find()) {
            return matcher1.group(1).trim();
        }

        Pattern pattern2 = Pattern.compile("^[■□]?단지명[\\s:：·-]*([^()]+?)(?:\\s*\\((.+)\\))?$");
        Matcher matcher2 = pattern2.matcher(name);
        if (matcher2.find()) {
            return matcher2.group(1).trim();
        }

        return null;
    }

    public static String extractAddress(String str) {
        String address = toNoWhiteSpace(str);

        // "□ 주택위치 : 서울특별시 은평구 대조동 2-9" 등의 주소 유형의 본문에서 주소만 분리하는 정규식
        Pattern pattern = Pattern.compile("^[■□]?주택위치[\\s:：·-]*([^()]+?)(?:\\s*\\((.+)\\))?$");
        Matcher matcher = pattern.matcher(address);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    public static String extractPageUrl(String str) {
        String url = toNoWhiteSpace(str);

        // "□ 페이지 URL : https://www.example.com" 등의 URL 유형의 본문에서 URL만 분리하는 정규식
        Pattern pattern = Pattern.compile("^[■□]?청약신청 페이지[\\s:：·-]*(https?://[^\\s]+)$");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

}
