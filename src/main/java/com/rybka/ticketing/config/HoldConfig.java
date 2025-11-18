package com.rybka.ticketing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(HoldProperties.class)
@Configuration
public class HoldConfig {
}
