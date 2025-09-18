package com.emailcrawler.util;

import java.util.regex.Pattern;

/**
 * 검증 관련 유틸리티 클래스
 */
public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private static final String[] INVALID_EMAIL_EXTENSIONS = {
            ".png", ".jpg", ".jpeg", ".gif", ".pdf", ".doc", ".docx", ".hwp"
    };

    /**
     * 이메일 주소가 유효한지 검증합니다.
     *
     * @param email 검증할 이메일
     * @return 유효하면 true
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty() || email.length() > 254) {
            return false;
        }

        email = email.toLowerCase().trim();

        // 파일 확장자가 포함된 이메일 제외
        for (String ext : INVALID_EMAIL_EXTENSIONS) {
            if (email.endsWith(ext)) {
                return false;
            }
        }

        // 기본 이메일 형식 검사
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * URL이 유효한지 검증합니다.
     *
     * @param url 검증할 URL
     * @return 유효하면 true
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        url = url.trim();
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * 문자열이 null이거나 비어있는지 확인합니다.
     *
     * @param str 확인할 문자열
     * @return null이거나 비어있으면 true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}