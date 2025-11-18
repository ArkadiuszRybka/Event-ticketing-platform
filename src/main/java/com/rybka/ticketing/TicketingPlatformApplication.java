package com.rybka.ticketing;

import com.rybka.ticketing.config.PaymentWebHookProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({PaymentWebHookProperties.class})
@EnableScheduling
public class TicketingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketingPlatformApplication.class, args);
	}

}
