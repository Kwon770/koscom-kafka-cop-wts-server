package com.koscom.kafkacop.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Base64;

public class FileUtils {
    public static String downloadAndConvertToBase64(String fileUrl) throws Exception {
        try (InputStream in = new URL(fileUrl).openStream()) {
            byte[] fileBytes = in.readAllBytes();
            return Base64.getEncoder().encodeToString(fileBytes);
        }
    }
}
