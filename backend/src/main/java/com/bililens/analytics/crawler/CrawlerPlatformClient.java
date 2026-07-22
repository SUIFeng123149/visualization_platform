package com.bililens.analytics.crawler;

import tools.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class CrawlerPlatformClient {

    private final CrawlerPlatformProperties properties;
    private final RestClient restClient;

    public CrawlerPlatformClient(CrawlerPlatformProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public JsonNode get(String path) {
        return request(restClient.get().uri(url(path))).retrieve().body(JsonNode.class);
    }

    public JsonNode post(String path, Object body) {
        return request(restClient.post().uri(url(path)).contentType(MediaType.APPLICATION_JSON).body(body)).retrieve().body(JsonNode.class);
    }

    private RestClient.RequestHeadersSpec<?> request(RestClient.RequestHeadersSpec<?> request) {
        if (StringUtils.hasText(properties.apiKey())) {
            request.header("x-api-key", properties.apiKey());
        }
        return request;
    }

    private String url(String path) {
        if (!properties.configured()) {
            throw new CrawlerNotConfiguredException();
        }
        return properties.baseUrl().replaceAll("/+$", "") + path;
    }
}
