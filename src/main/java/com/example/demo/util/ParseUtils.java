package com.example.demo.util;

import java.util.Collections;
import java.util.List;

public final class ParseUtils {

    private ParseUtils() {
        // Private constructor để ngăn việc tạo instance
    }

    /**
     * Chuyển đổi an toàn từ Object (Integer, Long, String, Double...) sang Long.
     * Tránh ClassCastException và NullPointerException.
     *
     * @param obj Giá trị cần chuyển đổi
     * @return Long hoặc null nếu không thể chuyển đổi
     */
    public static Long toLong(Object obj) {
        return toLong(obj, null);
    }

    /**
     * Chuyển đổi an toàn sang Long kèm giá trị mặc định nếu null hoặc lỗi.
     */
    public static Long toLong(Object obj, Long defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Chuyển đổi danh sách bất kỳ (List<?> từ JSON body) sang List<Long>.
     */
    public static List<Long> toLongList(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        if (obj instanceof List<?> list) {
            return list.stream()
                    .map(ParseUtils::toLong)
                    .filter(val -> val != null)
                    .toList();
        }
        return Collections.emptyList();
    }

    /**
     * Chuyển đổi an toàn sang Integer.
     */
    public static Integer toInteger(Object obj) {
        return toInteger(obj, null);
    }

    public static Integer toInteger(Object obj, Integer defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Chuyển đổi an toàn sang String (tránh NullPointerException).
     */
    public static String toString(Object obj) {
        return obj == null ? null : obj.toString();
    }
}
