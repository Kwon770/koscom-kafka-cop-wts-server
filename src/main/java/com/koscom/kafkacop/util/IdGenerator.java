package com.koscom.kafkacop.util;

import com.github.f4b6a3.ulid.UlidCreator;

public class IdGenerator {

    public static String generate() {
        return UlidCreator.getUlid().toString();
    }
}