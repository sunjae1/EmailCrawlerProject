package com.emailcrawler.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 텍스트에서 이메일을 추출하는 서비스
 */
public class EmailExtractorService {

    // 이메일 정규식 패턴들
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern OBFUSCATED_AT_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9._%+-]+\\s*\\[at\\]\\s*[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OBFUSCATED_PAREN_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9._%+-]+\\s*\\(at\\)\\s*[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", Pattern.CASE_INSENSITIVE);

    /**
     * 텍스트에서 이메일 주소를 추출합니다.
     *
     * @param text 검색할 텍스트
     * @return 발견된 이메일 리스트
     */
    public List<String> extractEmailsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> emails = new ArrayList<>();

        // 1. 일반적인 이메일 패턴
        addMatchesToList(emails, EMAIL_PATTERN.matcher(text));

        // 2. [at] 형태의 난독화된 이메일
        Matcher atMatcher = OBFUSCATED_AT_PATTERN.matcher(text);
        while (atMatcher.find()) {
            String email = atMatcher.group()
                    .replaceAll("\\[at\\]", "@")
                    .replaceAll("\\s", "");
            emails.add(email);
        }

        // 3. (at) 형태의 난독화된 이메일
        Matcher parenMatcher = OBFUSCATED_PAREN_PATTERN.matcher(text);
        while (parenMatcher.find()) {
            String email = parenMatcher.group()
                    .replaceAll("\\(at\\)", "@")
                    .replaceAll("\\s", "");
            emails.add(email);
        }

        return emails;
    }

    /**
     * 매처의 결과를 리스트에 추가
     */
    private void addMatchesToList(List<String> emails, Matcher matcher) {
        while (matcher.find()) {
            emails.add(matcher.group());
        }
    }
}