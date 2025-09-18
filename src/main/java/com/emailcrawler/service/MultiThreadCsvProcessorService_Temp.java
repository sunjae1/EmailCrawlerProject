package com.emailcrawler.service;

import com.emailcrawler.model.CsvRow;
import com.emailcrawler.util.CsvParser;
import com.emailcrawler.util.FileEncodingDetector;

import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadCsvProcessorService_Temp {
    private final FileEncodingDetector encodingDetector;
    private final CsvParser csvParser;
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);

    // 스레드 풀 설정
    private static final int THREAD_COUNT = 5; // 동시 실행 스레드 수
    private static final int DELAY_MS = 200;   // 대기 시간 (ms)

    public MultiThreadCsvProcessorService_Temp() {
        this.encodingDetector = new FileEncodingDetector();
        this.csvParser = new CsvParser();
    }

    /**
     * 멀티스레드로 CSV 파일 처리
     */
    public void processCsvFile(String csvPath) throws Exception {
        // 1. CSV 데이터 읽기
        String bestEncoding = encodingDetector.detectEncoding(csvPath);
        System.out.println("✅ 최적 인코딩: " + bestEncoding);

        List<CsvRow> rows = csvParser.parseCsvFile(csvPath, bestEncoding);

        if (rows.isEmpty()) {
            throw new IllegalStateException("CSV 데이터를 읽을 수 없습니다.");
        }

        // 🔍 디버깅: 헤더 정보 확인
        if (!rows.isEmpty() && rows.get(0).isHeader()) {
            CsvRow header = rows.get(0);
            System.out.println("📋 헤더: " + String.join(", ", header.getValues()));
            System.out.println("📋 Email 컬럼 인덱스: " + header.getEmailCol());
        }

        // 2. 헤더 제외한 데이터만 추출
        List<CsvRow> dataRows = rows.subList(1, rows.size());
        int totalRows = dataRows.size();

        System.out.println("📊 총 " + totalRows + "개 회사 데이터 발견");
        System.out.println("🚀 " + THREAD_COUNT + "개 스레드로 병렬 처리 시작");

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
     * 멀티스레드로 크롤링 실행 (순서 보존)
     */
    private void crawlWithMultipleThreads(List<CsvRow> dataRows) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            // 🔥 인덱스와 함께 작업 제출
            for (int i = 0; i < dataRows.size(); i++) {
                CsvRow row = dataRows.get(i);
                executor.submit(new CrawlingTask(row, i + 1));
            }

            // 모든 작업 완료 대기
            executor.shutdown();

            // 깔끔한 진행 상황 모니터링
            monitorProgressClean(dataRows.size());

            // 최대 3시간 대기
            if (!executor.awaitTermination(3, TimeUnit.HOURS)) {
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
     * 개별 크롤링 작업 클래스 (조용한 버전)
     */
    private class CrawlingTask implements Runnable {
        private final CsvRow row;
        private final int index;

        public CrawlingTask(CsvRow row, int index) {
            this.row = row;
            this.index = index;
        }

        @Override
        public void run() {
            try {
                if (row.getWebsite().isEmpty()) {
                    row.setFoundEmail("X");
                } else {
                    // 실제 크롤링 실행
                    EmailCrawlerService emailCrawler = new EmailCrawlerService();
                    String foundEmail = emailCrawler.crawlWebsiteForEmail(row.getWebsite());
                    row.setFoundEmail(foundEmail.isEmpty() ? "X" : foundEmail);

                    if (!foundEmail.isEmpty()) {
                        // 🔥 이메일 발견 시에만 출력
                        synchronized(System.out) {
                            System.out.printf("✅ [%d] %s → %s\n",
                                    index, row.getCompany(), foundEmail);
                        }
                        successCount.incrementAndGet();
                    }

                    // 서버 부하 방지 대기
                    Thread.sleep(DELAY_MS);
                }

                completedCount.incrementAndGet();

            } catch (Exception e) {
                row.setFoundEmail("X");
                completedCount.incrementAndGet();

                // 심각한 오류만 출력
                synchronized(System.out) {
                    System.err.printf("❌ [%d] %s: %s\n", index, row.getCompany(), e.getMessage());
                }
            }
        }
    }

    /**
     * 깔끔한 실시간 진행 상황 표시
     */
    private void monitorProgressClean(int totalRows) {
        Thread progressMonitor = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();

                while (completedCount.get() < totalRows) {
                    // 🔥 화면 지우기
                    clearScreen();

                    int completed = completedCount.get();
                    int success = successCount.get();
                    double progress = (double) completed / totalRows * 100;

                    // 경과/예상 시간 계산
                    long elapsedSec = (System.currentTimeMillis() - startTime) / 1000;
                    long estimatedTotalSec = completed > 0 ? (elapsedSec * totalRows / completed) : 0;
                    long remainingSec = Math.max(0, estimatedTotalSec - elapsedSec);

                    // 🔥 고정된 위치에 정보 표시
                    System.out.println("🕷️ ═══════ 이메일 크롤러 ═══════");
                    System.out.printf("📊 진행률: %,d / %,d (%.1f%%)\n", completed, totalRows, progress);
                    System.out.printf("📧 이메일: %,d개 (%.1f%% 성공)\n", success, completed > 0 ? (double)success/completed*100 : 0);

                    // 프로그레스 바 표시
                    showProgressBar(progress, 30);

                    System.out.printf("⏰ 경과: %s | 남은시간: %s\n",
                            formatTime(elapsedSec), formatTime(remainingSec));
                    System.out.printf("⚡ 속도: %.1f 사이트/분\n",
                            completed > 0 ? (double)completed * 60 / elapsedSec : 0);

                    System.out.println("═".repeat(35));
                    System.out.println("💡 Ctrl+C로 중단 가능");

                    Thread.sleep(2000); // 2초마다 업데이트
                }

                // 완료 시 최종 화면
                clearScreen();
                System.out.println("🎉 ═══════ 크롤링 완료! ═══════");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        progressMonitor.setDaemon(true);
        progressMonitor.start();
    }

    /**
     * 화면 지우기
     */
    private void clearScreen() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                // Windows
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // Unix/Linux/Mac
                System.out.print("\033[2J\033[H");
                System.out.flush();
            }
        } catch (Exception e) {
            // 실패 시 여러 줄 출력으로 화면 밀어내기
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    /**
     * 프로그레스 바 표시
     */
    private void showProgressBar(double percentage, int length) {
        int filled = (int) (percentage / 100 * length);
        StringBuilder bar = new StringBuilder("📈 [");

        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }

        bar.append(String.format("] %.1f%%", percentage));
        System.out.println(bar.toString());
    }

    /**
     * 시간 포맷
     */
    private String formatTime(long seconds) {
        if (seconds < 0) return "--:--";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
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
        System.out.printf("📊 처리된 회사: %,d/%,d\n", completed, totalRows);
        System.out.printf("📧 이메일 발견: %,d개 (성공률: %.1f%%)\n", success, successRate);
        System.out.printf("⏱️ 총 소요시간: %d시간 %d분 %d초\n", hours, minutes, seconds);

        if (totalRows > 0) {
            double avgTimePerSite = (double) totalTimeMs / totalRows / 1000;
            System.out.printf("⚡ 사이트당 평균 처리 시간: %.2f초\n", avgTimePerSite);
        }
    }

    // 기타 헬퍼 메소드들...
    private String generateOutputPath(String originalPath) {
        if (originalPath.toLowerCase().endsWith(".csv")) {
            return originalPath.substring(0, originalPath.length() - 4) + "_updated.csv";
        } else {
            return originalPath + "_updated.csv";
        }
    }

    /**
     * 🔥 수정된 CSV 저장 메소드 (디버깅 추가)
     */
    private void saveCsvData(List<CsvRow> rows, String outputPath) throws Exception {
        System.out.println("🔍 저장 시작: " + outputPath);

        try (FileWriter writer = new FileWriter(outputPath, java.nio.charset.StandardCharsets.UTF_8)) {
            // UTF-8 BOM 추가 (Excel 호환)
            writer.write('\ufeff');

            for (int i = 0; i < rows.size(); i++) {
                CsvRow row = rows.get(i);

                if (row.isHeader()) {
                    // 헤더 그대로 출력
                    String headerLine = String.join(",", row.getValues());
                    writer.write(headerLine + "\n");
                    System.out.println("📋 헤더 저장: " + headerLine);
                } else {
                    // 🔥 중요: 배열 복사해서 수정
                    String[] updatedValues = row.getValues().clone();

                    // 🔍 디버깅 정보
                    if (i <= 5) { // 처음 5개만 디버깅 출력
                        System.out.printf("🔍 [%d] %s: emailCol=%d, foundEmail='%s'\n",
                                i, row.getCompany(), row.getEmailCol(), row.getFoundEmail());
                    }

                    if (row.getEmailCol() >= 0 && row.getEmailCol() < updatedValues.length) {
                        updatedValues[row.getEmailCol()] = row.getFoundEmail();
                        if (i <= 5) {
                            System.out.printf("✅ [%d] Email 컬럼 업데이트: '%s'\n", i, row.getFoundEmail());
                        }
                    } else {
                        if (i <= 5) {
                            System.out.printf("❌ [%d] Email 컬럼 인덱스 오류: %d (배열 길이: %d)\n",
                                    i, row.getEmailCol(), updatedValues.length);
                        }
                    }

                    // CSV 형식으로 출력
                    for (int j = 0; j < updatedValues.length; j++) {
                        if (j > 0) writer.write(",");
                        writer.write(escapeCsvField(updatedValues[j]));
                    }
                    writer.write("\n");
                }
            }
        }

        System.out.println("💾 파일 저장 완료: " + outputPath);

        // 🔍 저장된 파일 검증
        System.out.println("📂 저장된 파일 위치 확인: ");
        System.out.println("   " + new java.io.File(outputPath).getAbsolutePath());
    }

    /**
     * CSV 필드 이스케이프
     */
    private String escapeCsvField(String field) {
        if (field == null) return "";

        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}