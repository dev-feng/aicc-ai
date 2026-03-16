package com.callcenter.core;

import com.callcenter.core.config.CoreProperties;
import com.callcenter.core.config.FreeSwitchConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 第一期核心呼叫模块启动入口。
 */
@SpringBootApplication(scanBasePackages = "com.callcenter")
@EnableConfigurationProperties({CoreProperties.class, FreeSwitchConfig.class})
public class CallCoreApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args startup args
     */
    public static void main(String[] args) {
        SpringApplication.run(CallCoreApplication.class, args);
    }
}
