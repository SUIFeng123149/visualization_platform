package com.bililens.analytics.crawler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics.crawler")
public record CrawlerPlatformProperties(String baseUrl, String apiKey) {

    public boolean configured() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
