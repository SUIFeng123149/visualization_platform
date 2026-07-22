package com.bililens.analytics.crawler;

public class CrawlerNotConfiguredException extends RuntimeException {
    public CrawlerNotConfiguredException() {
        super("爬虫平台尚未接入，请配置 CRAWLER_BASE_URL 后重试");
    }
}
