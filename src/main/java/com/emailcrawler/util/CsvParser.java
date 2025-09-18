package com.emailcrawler.util;

import com.emailcrawler.model.CsvRow;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 파일 파싱 유틸리티
 */
public class CsvParser {

    /**
     * CSV 파일을 파싱하여 CsvRow 리스트로 반환합니다.
     *
     * @param filePath CSV 파일 경로
     * @param encoding 파일 인코딩
     * @return CsvRow 리스트
     * @throws Exception 파싱 중 오류 발생 시
     */
    public List<CsvRow> parseCsvFile(String filePath, String encoding) throws Exception {
        List<CsvRow> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), encoding))) {

            String line;
            boolean isHeader = true;
            int companyCol = -1, websiteCol = -1, emailCol = -1;

            while ((line = br.readLine()) != null) {
                // BOM 제거
                if (line.startsWith("\ufeff")) {
                    line = line.substring(1);
                }

                String[] values = parseCsvLine(line);

                if (isHeader) {
                    // 헤더에서 컬럼 인덱스 찾기
                    for (int i = 0; i < values.length; i++) {
                        String col = values[i].toLowerCase().trim();
                        if (col.contains("company") || col.contains("회사") || col.contains("업체")) {
                            companyCol = i;
                        } else if (col.contains("website") || col.contains("홈페이지") || col.contains("url") || col.contains("사이트")) {
                            websiteCol = i;
                        } else if (col.contains("email") || col.contains("이메일") || col.contains("메일")) {
                            emailCol = i;
                        }
                    }

                    System.out.println("📋 컬럼 매핑:");
                    System.out.println("   Company: " + (companyCol >= 0 ? (companyCol + 1) + "번째 (" + values[companyCol] + ")" : "찾지 못함"));
                    System.out.println("   Website: " + (websiteCol >= 0 ? (websiteCol + 1) + "번째 (" + values[websiteCol] + ")" : "찾지 못함"));
                    System.out.println("   Email: " + (emailCol >= 0 ? (emailCol + 1) + "번째 (" + values[emailCol] + ")" : "찾지 못함"));

                    rows.add(new CsvRow(values, true)); // 헤더 저장
                    isHeader = false;
                } else {
                    // 데이터 행 처리
                    String company = companyCol >= 0 && companyCol < values.length ? values[companyCol].trim() : "";
                    String website = websiteCol >= 0 && websiteCol < values.length ? values[websiteCol].trim() : "";
                    String email = emailCol >= 0 && emailCol < values.length ? values[emailCol].trim() : "";

                    CsvRow row = new CsvRow(values, false);
                    row.setCompany(company);
                    row.setWebsite(website);
                    row.setOriginalEmail(email);
                    row.setCompanyCol(companyCol);
                    row.setWebsiteCol(websiteCol);
                    row.setEmailCol(emailCol);

                    rows.add(row);
                }
            }
        }

        return rows;
    }

    /**
     * CSV 라인을 파싱합니다.
     *
     * @param line CSV 라인
     * @return 파싱된 필드 배열
     */
    public String[] parseCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 이스케이프된 따옴표 ""
                    currentField.append('"');
                    i++; // 다음 따옴표 스킵
                } else {
                    // 따옴표 시작/끝
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 필드 구분자
                result.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // 마지막 필드 추가
        result.add(currentField.toString().trim());
        return result.toArray(new String[0]);
    }

    /**
     * CSV 필드를 안전하게 이스케이프 처리합니다.
     *
     * @param field 이스케이프할 필드
     * @return 이스케이프된 필드
     */
    public String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // 쉼표, 따옴표, 개행문자가 포함된 경우 따옴표로 감싸기
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }
}