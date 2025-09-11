package com.koscom.kafkacop.web.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;

@ConfigurationProperties(prefix = "app.gpt")
@ConfigurationPropertiesBinding
public record GptAgentProperties(
        String apiKey
) {
}
