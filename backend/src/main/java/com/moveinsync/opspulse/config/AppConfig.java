package com.moveinsync.opspulse.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OpsPulseProperties.class, SarvamProperties.class, OpenAiProperties.class})
public class AppConfig {
}
