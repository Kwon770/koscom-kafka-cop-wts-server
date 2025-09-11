package com.koscom.kafkacop.util;

import java.util.concurrent.ThreadLocalRandom;

import com.github.f4b6a3.ulid.UlidCreator;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class IdGenerator {
    public static String generateAlphanumericString(int length) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'

        return ThreadLocalRandom.current()
                .ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static String generateUlid() {
        return UlidCreator.getMonotonicUlid().toString();
    }
}
