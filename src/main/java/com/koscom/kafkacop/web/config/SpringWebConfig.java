package com.koscom.kafkacop.web.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SpringWebConfig implements WebMvcConfigurer {
	final WebRequestInterceptor webRequestInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(webRequestInterceptor);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins(
				"http://localhost:3000",
				"http://localhost:5173",
				"https://consume.kafka.dwer.kr",
				"http://consume.kafka.dwer.kr"
				)
			.allowedMethods("GET", "POST", "PUT", "DELETE")
			.allowCredentials(true)
			.maxAge(3600);
	}
}
