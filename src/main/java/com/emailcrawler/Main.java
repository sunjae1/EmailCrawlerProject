
package com.emailcrawler;

import com.emailcrawler.service.CsvProcessorService;

import java.util.Scanner;

/**
 * 이메일 크롤러 메인 클래스
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("🕷️ === CSV 이메일 업데이터 크롤러 === 🕷️");
        System.out.println("버전: 1.0.0");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        try {
            // CSV 파일 경로 입력
            System.out.print("📁 CSV 파일 경로를 입력하세요: ");
            String csvPath = scanner.nextLine().trim();

            if (csvPath.isEmpty()) {
                System.out.println("❌ 파일 경로가 입력되지 않았습니다.");
                return;
            }

            // CSV 처리 서비스 실행
            CsvProcessorService processor = new CsvProcessorService();
            processor.processCsvFile(csvPath);

        } catch (Exception e) {
            System.err.println("❌ 프로그램 실행 중 오류가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }

        System.out.println("\n👋 프로그램이 종료되었습니다.");
    }
}