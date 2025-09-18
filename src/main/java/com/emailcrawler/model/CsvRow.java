package com.emailcrawler.model;


/**
 * CSV 행 데이터를 담는 모델 클래스
 */
public class CsvRow {
    private String[] values;
    private boolean isHeader;
    private String company = "";
    private String website = "";
    private String originalEmail = "";
    private String foundEmail = "";
    private int companyCol = -1;
    private int websiteCol = -1;
    private int emailCol = -1;

    public CsvRow(String[] values, boolean isHeader) {
        this.values = values.clone();
        this.isHeader = isHeader;
    }

    // Getters and Setters
    public String[] getValues() { return values.clone(); }
    public void setValues(String[] values) { this.values = values.clone(); }

    public boolean isHeader() { return isHeader; }
    public void setHeader(boolean header) { isHeader = header; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getOriginalEmail() { return originalEmail; }
    public void setOriginalEmail(String originalEmail) { this.originalEmail = originalEmail; }

    public String getFoundEmail() { return foundEmail; }
    public void setFoundEmail(String foundEmail) { this.foundEmail = foundEmail; }

    public int getCompanyCol() { return companyCol; }
    public void setCompanyCol(int companyCol) { this.companyCol = companyCol; }

    public int getWebsiteCol() { return websiteCol; }
    public void setWebsiteCol(int websiteCol) { this.websiteCol = websiteCol; }

    public int getEmailCol() { return emailCol; }
    public void setEmailCol(int emailCol) { this.emailCol = emailCol; }

    @Override
    public String toString() {
        return "CsvRow{" +
                "company='" + company + '\'' +
                ", website='" + website + '\'' +
                ", foundEmail='" + foundEmail + '\'' +
                '}';
    }
}
