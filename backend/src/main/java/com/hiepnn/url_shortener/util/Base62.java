package com.hiepnn.url_shortener.util;

import java.util.Arrays;

public class Base62 {
    private static final String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = CHARACTERS.length();
    // 1. Khởi tạo mảng Lookup Table kích thước 256
    private static final int[] INDEX_LOOKUP = new int[256];

    static {
        // Gán tất cả phần tử mặc định là -1 (Ký tự không hợp lệ)
        Arrays.fill(INDEX_LOOKUP, -1);

        // Đánh dấu vị trí thực sự cho 62 ký tự hợp lệ
        for (int i = 0; i < CHARACTERS.length(); i++) {
            char c = CHARACTERS.charAt(i);
            INDEX_LOOKUP[c] = i; // Vị trí ASCII của 'c' sẽ lưu giá trị i
        }
    }

    public static String encode(long value) {
        if (value == 0) {
            return String.valueOf(CHARACTERS.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(CHARACTERS.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String string) {
        long value = 0;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            // 2. Tra cứu O(1) trực tiếp từ mảng thay vì dùng CHARACTERS.indexOf(c)
            int digit = (c < 256) ? INDEX_LOOKUP[c] : -1;
            if (digit < 0) {
                throw new IllegalArgumentException("Ký tự không hợp lệ trong chuỗi Base62: " + c);
            }
            value = value * BASE + digit;
        }
        return value;
    }
}
