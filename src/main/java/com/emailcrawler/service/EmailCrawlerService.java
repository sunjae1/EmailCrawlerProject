package com.emailcrawler.service;

import com.emailcrawler.util.ValidationUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 웹사이트 크롤링 서비스
 */
public class EmailCrawlerService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 15000; // 15초
    private static final Pattern MAILTO_PATTERN = Pattern.compile("mailto:([^?&\\s]+)", Pattern.CASE_INSENSITIVE);

    private final EmailExtractorService emailExtractor;

    public EmailCrawlerService() {
        this.emailExtractor = new EmailExtractorService();
    }

    /**
     * 웹사이트에서 이메일을 크롤링합니다.
     *
     * @param url 크롤링할 웹사이트 URL
     * @return 발견된 첫 번째 유효한 이메일, 없으면 빈 문자열
     */
    public String crawlWebsiteForEmail(String url) {
        if (!ValidationUtils.isValidUrl(url)) {
            System.out.println("⚠️ 유효하지 않은 URL: " + url);
            return "";
        }

        try {
            // 웹페이지 가져오기
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .get();

            // 1. mailto 링크에서 우선 추출
            String mailtoEmail = extractFromMailtoLinks(doc);
            if (!mailtoEmail.isEmpty()) {
                System.out.println("📧 mailto 링크에서 발견: " + mailtoEmail);
                return mailtoEmail;
            }

            // 2. 페이지 텍스트에서 이메일 추출
            String pageText = doc.text();
            List<String> emails = emailExtractor.extractEmailsFromText(pageText);

            // 첫 번째 유효한 이메일 반환
            for (String email : emails) {
                email = email.toLowerCase().trim();
                if (ValidationUtils.isValidEmail(email)) {
//                    System.out.println("📧 페이지에서 발견: " + email);
                    return email;
                }
            }

//            System.out.println("❌ 유효한 이메일을 찾을 수 없음");
            return "";

        } catch (Exception e) {
            System.out.println("⚠️ 크롤링 오류: " + e.getMessage());
            return "";
        }
    }

    /**
     * mailto 링크에서 이메일 추출
     */
    private String extractFromMailtoLinks(Document doc) {
        Elements mailtoLinks = doc.select("a[href^=mailto:]");

        for (Element link : mailtoLinks) {
            String href = link.attr("href");
            Matcher matcher = MAILTO_PATTERN.matcher(href);
            if (matcher.find()) {
                String email = matcher.group(1).toLowerCase().trim();
                if (ValidationUtils.isValidEmail(email)) {
                    return email;
                }
            }
        }

        return "";
    }
}