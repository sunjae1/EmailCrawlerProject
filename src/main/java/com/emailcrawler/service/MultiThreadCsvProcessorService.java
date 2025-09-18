package com.emailcrawler.service;

import com.emailcrawler.model.CsvRow;
import com.emailcrawler.util.CsvParser;
import com.emailcrawler.util.FileEncodingDetector;

import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 멀티스레드 기반 CSV 처리 서비스
 */
public class MultiThreadCsvProcessorService {

    private final FileEncodingDetector encodingDetector;
    private final CsvParser csvParser;
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);

    // 스레드 풀 설정
    private static final int THREAD_COUNT = 5; // 동시 실행 스레드 수
    private static final int DELAY_MS = 200;   // 대기 시간 (ms)

    public MultiThreadCsvProcessorService() {
        this.encodingDetector = new FileEncodingDetector();
        this.csvParser = new CsvParser();
    }

    /**
     * 멀티스레드로 CSV 파일 처리
     */
    public void processCsvFile(String csvPath) throws Exception {
        // 1. CSV 데이터 읽기
        String bestEncoding = encodingDetector.detectEncoding(csvPath);
        List<CsvRow> rows = csvParser.parseCsvFile(csvPath, bestEncoding);

        if (rows.isEmpty()) {
            throw new IllegalStateException("CSV 데이터를 읽을 수 없습니다.");
        }

        // 2. 헤더 제외한 데이터만 추출
        List<CsvRow> dataRows = rows.subList(1, rows.size());
        int totalRows = dataRows.size();

        System.out.println("📊 총 " + totalRows + "개 회사 데이터 발견");
        System.out.println("🚀 " + THREAD_COUNT + "개 스레드로 병렬 처리 시작");
        System.out.println("═".repeat(60));

        long startTime = System.currentTimeMillis();

        // 3. 멀티스레드 실행
        crawlWithMultipleThreads(dataRows);

        long endTime = System.currentTimeMillis();
        long totalTimeMs = endTime - startTime;

        // 4. 결과 저장
        String outputPath = generateOutputPath(csvPath);
        saveCsvData(rows, outputPath);

        // 5. 통계 출력
        printStatistics(totalRows, totalTimeMs);
    }

    /**
     * 멀티스레드로 크롤링 실행
     */
    private void crawlWithMultipleThreads(List<CsvRow> dataRows) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            // 각 행을 별도 작업으로 제출
            for (CsvRow row : dataRows) {
                executor.submit(new CrawlingTask(row));
            }

            // 모든 작업 완료 대기
            executor.shutdown();

            // 진행 상황 모니터링
            monitorProgress(dataRows.size());

            // 최대 1시간 대기 (20,000개 기준)
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.out.println("⚠️ 타임아웃! 일부 작업이 완료되지 않았습니다.");
                executor.shutdownNow();
            }

        } finally {
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * 개별 크롤링 작업 클래스
     */
    private class CrawlingTask implements Runnable {
        private final CsvRow row;

        public CrawlingTask(CsvRow row) {
            this.row = row;
        }

        @Override
        public void run() {
            try {
                String threadName = Thread.currentThread().getName();
                int current = completedCount.incrementAndGet();

                if (row.getWebsite().isEmpty()) {
                    System.out.printf("[%s] [%d] %s - 웹사이트 URL 없음\n",
                            threadName, current, row.getCompany());
                    row.setFoundEmail("X");
                } else {
                    System.out.printf("[%s] [%d] %s - 크롤링 시작\n",
                            threadName, current, row.getCompany());

                    // 실제 크롤링 실행
                    EmailCrawlerService emailCrawler = new EmailCrawlerService();
                    String foundEmail = emailCrawler.crawlWebsiteForEmail(row.getWebsite());
                    row.setFoundEmail(foundEmail.isEmpty() ? "X" : foundEmail);

                    if (!foundEmail.isEmpty()) {
                        System.out.printf("[%s] [%d] %s - ✅ 이메일 발견: %s\n",
                                threadName, current, row.getCompany(), foundEmail);
                        successCount.incrementAndGet();
                    } else {
                        System.out.printf("[%s] [%d] %s - ❌ 이메일 없음\n",
                                threadName, current, row.getCompany());
                    }

                    // 서버 부하 방지 대기
                    Thread.sleep(DELAY_MS);
                }

            } catch (Exception e) {
                System.err.printf("❌ 크롤링 오류 [%s]: %s\n", row.getCompany(), e.getMessage());
                row.setFoundEmail("X");
            }
        }
    }

    /**
     * 진행 상황 모니터링
     */
    private void monitorProgress(int totalRows) {
        Thread progressMonitor = new Thread(() -> {
            try {
                while (completedCount.get() < totalRows) {
                    Thread.sleep(5000); // 5초마다 진행 상황 출력

                    int completed = completedCount.get();
                    int success = successCount.get();
                    double progress = (double) completed / totalRows * 100;

                    System.out.printf("\n📊 진행 상황: %d/%d (%.1f%%) | 이메일 발견: %d개\n",
                            completed, totalRows, progress, success);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        progressMonitor.setDaemon(true);
        progressMonitor.start();
    }

    /**
     * 최종 통계 출력
     */
    private void printStatistics(int totalRows, long totalTimeMs) {
        int completed = completedCount.get();
        int success = successCount.get();
        double successRate = (double) success / completed * 100;

        long hours = totalTimeMs / (1000 * 60 * 60);
        long minutes = (totalTimeMs / (1000 * 60)) % 60;
        long seconds = (totalTimeMs / 1000) % 60;

        System.out.println("\n═".repeat(60));
        System.out.println("🎉 처리 완료!");
        System.out.printf("📊 처리된 회사: %d/%d\n", completed, totalRows);
        System.out.printf("📧 이메일 발견: %d개 (성공률: %.1f%%)\n", success, successRate);
        System.out.printf("⏱️ 총 소요시간: %d시간 %d분 %d초\n", hours, minutes, seconds);

        if (totalRows > 0) {
            double avgTimePerSite = (double) totalTimeMs / totalRows / 1000;
            System.out.printf("⚡ 사이트당 평균 처리 시간: %.2f초\n", avgTimePerSite);
        }
    }

    // 기타 헬퍼 메소드들.
    private String generateOutputPath(String originalPath) {
        return originalPath.replace(".csv", "_updated.csv");
    }

    private void saveCsvData(List<CsvRow> rows, String outputPath) throws Exception {
        try (FileWriter writer = new FileWriter(outputPath, java.nio.charset.StandardCharsets.UTF_8)) {
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
                        writer.write(escapeCsvField(updatedValues[i]));
                    }
                    writer.write("\n");
                }
            }
        }

        System.out.println("💾 파일 저장 완료: " + outputPath);
    }

    // escapeCsvField 메소드도 추가 필요
    private String escapeCsvField(String field) {
        if (field == null) return "";

        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}