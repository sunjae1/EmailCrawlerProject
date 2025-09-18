package com.emailcrawler.service;

import com.emailcrawler.model.CsvRow;
import com.emailcrawler.util.CsvParser;
import com.emailcrawler.util.FileEncodingDetector;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CSV 파일 처리 메인 서비스
 */
public class CsvProcessorService {

    private final FileEncodingDetector encodingDetector;
    private final CsvParser csvParser;
    private final EmailCrawlerService emailCrawler;

    public CsvProcessorService() {
        this.encodingDetector = new FileEncodingDetector();
        this.csvParser = new CsvParser();
        this.emailCrawler = new EmailCrawlerService();
    }

    /**
     * CSV 파일을 처리하여 이메일을 크롤링하고 업데이트합니다.
     *
     * @param csvPath CSV 파일 경로
     * @throws Exception 처리 중 오류 발생 시
     */
    public void processCsvFile(String csvPath) throws Exception {
        // 1. 파일 존재 확인
        File csvFile = new File(csvPath);
        if (!csvFile.exists()) {
            throw new IllegalArgumentException("파일을 찾을 수 없습니다: " + csvPath);
        }

        System.out.println("📋 CSV 파일 분석 중...");

        // 2. 최적 인코딩 감지
        String bestEncoding = encodingDetector.detectEncoding(csvPath);
        System.out.println("✅ 최적 인코딩: " + bestEncoding);

        // 3. CSV 데이터 읽기
        List<CsvRow> rows = csvParser.parseCsvFile(csvPath, bestEncoding);

        if (rows.isEmpty()) {
            throw new IllegalStateException("CSV 데이터를 읽을 수 없습니다.");
        }

        System.out.println("📊 총 " + (rows.size() - 1) + "개 회사 데이터 발견");

        // 4. 각 웹사이트 크롤링
        crawlAndUpdateEmails(rows);

        // 5. 업데이트된 CSV 파일 저장
        String outputPath = generateOutputPath(csvPath);
        saveCsvData(rows, outputPath);

        System.out.println("═".repeat(60));
        System.out.println("🎉 처리 완료! 업데이트된 파일: " + outputPath);
    }

    /**
     * 웹사이트들을 크롤링해서 이메일 찾기
     */
    private void crawlAndUpdateEmails(List<CsvRow> rows) {
        System.out.println("═".repeat(60));

        int count = 0;
        int total = rows.size() - 1; // 헤더 제외

        for (int i = 1; i < rows.size(); i++) { // 헤더 스킵
            CsvRow row = rows.get(i);
            count++;

            System.out.printf("\n[%d/%d] %s\n", count, total, row.getCompany());
            System.out.println("🌐 웹사이트: " + row.getWebsite());

            if (row.getWebsite().isEmpty()) {
                System.out.println("❌ 웹사이트 URL이 비어있음"); //2초 대기 없이 넘기게 수정.
                row.setFoundEmail("X");
                continue; //🔥 NEW: 대기 없이 바로 다음 반복으로
            } else {
                // 웹사이트 크롤링
                String foundEmail = emailCrawler.crawlWebsiteForEmail(row.getWebsite());
                row.setFoundEmail(foundEmail.isEmpty() ? "X" : foundEmail);

                if (!foundEmail.isEmpty()) {
                    System.out.println("✅ 이메일 발견!");
                } else {
                    System.out.println("❌ 이메일을 찾을 수 없음");
                }
            }

            // 서버 부하 방지 (마지막이 아닌 경우만)
            if (i < rows.size() - 1) {
                try {
                    System.out.println("⏳ 0.2초 대기...");
                    //20,000개 데이터 --> Thread.sleep(2000); 14시간
                    //20,000개 데이터 --> Thread.sleep(200); 3~6시간
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }


        }
    }

    /**
     * 출력 파일 경로 생성
     */
    private String generateOutputPath(String originalPath) {
        if (originalPath.toLowerCase().endsWith(".csv")) {
            return originalPath.substring(0, originalPath.length() - 4) + "_updated.csv";
        } else {
            return originalPath + "_updated.csv";
        }
    }

    /**
     * 업데이트된 CSV 데이터 저장
     */
    private void saveCsvData(List<CsvRow> rows, String outputPath) throws Exception {
        try (FileWriter writer = new FileWriter(outputPath, StandardCharsets.UTF_8)) {
            // UTF-8 BOM 추가 (Excel 호환)
            writer.write('\ufeff');

            for (CsvRow row : rows) {
                if (row.isHeader()) {
                    // 헤더 그대로 출력
                    writer.write(String.join(",", row.getValues()) + "\n");
                } else {
                    // 데이터 행에서 Email 컬럼 업데이트
                    String[] updatedValues = row.getValues();
                    if (row.getEmailCol() >= 0 && row.getEmailCol() < updatedValues.length) {
                        updatedValues[row.getEmailCol()] = row.getFoundEmail();
                    }

                    // CSV 형식으로 출력
                    for (int i = 0; i < updatedValues.length; i++) {
                        if (i > 0) writer.write(",");
                        writer.write(csvParser.escapeCsvField(updatedValues[i]));
                    }
                    writer.write("\n");
                }
            }
        }

        System.out.println("💾 파일 저장 완료: " + outputPath);
    }
}
