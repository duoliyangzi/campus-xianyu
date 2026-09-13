package com.campus.xianyu;

import com.campus.xianyu.ai.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class XianyuApplication {

	public static void main(String[] args) {
		SpringApplication.run(XianyuApplication.class, args);
	}

}
