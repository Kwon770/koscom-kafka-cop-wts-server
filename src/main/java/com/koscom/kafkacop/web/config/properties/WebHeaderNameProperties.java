package com.koscom.kafkacop.web.config.properties;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;

@ConfigurationPropertiesBinding
public record WebHeaderNameProperties(
        String accessToken,
        String appVersionHeader,
        String platformHeader,
        String userIdHeader,
        String appKeyHeader
) {
    public WebHeaderNameProperties {
        if (accessToken == null) throw new IllegalArgumentException("accessToken cannot be null");
    }
}
