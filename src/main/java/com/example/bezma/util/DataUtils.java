package com.example.bezma.util;

import com.github.slugify.Slugify;

import java.util.UUID;

public class DataUtils {

    // Khởi tạo Slugify
    private static final Slugify slg = Slugify.builder().build();

    /**
     * Tạo slug từ chuỗi đầu vào
     */
    public static String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return slg.slugify(input);
    }

    /**
     * Tạo slug kèm hậu tố ngẫu nhiên để tránh trùng lặp tuyệt đối trong DB
     */
    public static String generateUniqueSlug(String input) {
        String baseSlug = generateSlug(input);
        return baseSlug + "-" + UUID.randomUUID().toString().substring(0, 5);
    }
}