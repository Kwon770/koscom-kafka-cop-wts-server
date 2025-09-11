package com.koscom.kafkacop.web.config.properties;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "app.web")
@ConfigurationPropertiesBinding
public record WebProperties(
        String[] urlNoLogging
) {
    public boolean isNoLoggable(String path) {
        for (String pattern : urlNoLogging) {
            if (isMatchPattern(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMatchPattern(String pattern, String input) {
        String regex = pattern.replace("*", ".*");
        return Pattern.matches(regex, input);
    }
}
