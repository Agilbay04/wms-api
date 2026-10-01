package com.warehousing.wmsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class WmsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WmsApiApplication.class, args);
	}

}
