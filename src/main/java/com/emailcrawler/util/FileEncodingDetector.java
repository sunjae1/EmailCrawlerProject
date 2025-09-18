package com.emailcrawler.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * 파일 인코딩 자동 감지 유틸리티
 */
public class FileEncodingDetector {

    private static final String[] ENCODINGS = {"CP1252", "EUC-KR", "MS949", "UTF-8", "ISO-8859-1"};

    /**
     * CSV 파일의 최적 인코딩을 감지합니다.
     *
     * @param filePath 파일 경로
     * @return 최적 인코딩
     */
    public String detectEncoding(String filePath) {
        System.out.println("🔍 인코딩 감지 중...");

 /*       for (String encoding : ENCODINGS) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), encoding))) {

                String headerLine = br.readLine();
                String dataLine = br.readLine();

                if (headerLine != null && dataLine != null) {
                    // BOM 제거
                    if (headerLine.startsWith("\ufeff")) {
                        headerLine = headerLine.substring(1);
                    }

                    System.out.println(encoding + ": " + headerLine + " | " + dataLine.substring(0, Math.min(dataLine.length(), 30)) + "...");

                    // 한글이 제대로 읽혔거나 깨진 문자가 없으면 선택
                    if (containsValidKorean(dataLine) || !containsBrokenChars(dataLine)) {
                        System.out.println("✅ " + encoding + " 선택됨");
                        return encoding;
                    }
                }
            } catch (Exception e) {
                System.out.println(encoding + ": 읽기 실패 - " + e.getMessage());
            }
        }
*/
//        System.out.println("⚠️ 최적 인코딩을 찾지 못함. UTF-8 사용");
        System.out.println("⚠️ 최적 인코딩을 찾지 못함. EUC-KR 사용");
//        return "UTF-8";
        return "EUC-KR";
    }

    /**
     * 텍스트에 유효한 한글이 포함되어 있는지 확인
     */
    private boolean containsValidKorean(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7AF) { // 한글 유니코드 범위
                return true;
            }
        }
        return false;
    }

    /**
     * 텍스트에 깨진 문자가 있는지 확인
     */
    private boolean containsBrokenChars(String text) {
        return text != null && (text.contains("�") || text.contains("??"));
    }
}