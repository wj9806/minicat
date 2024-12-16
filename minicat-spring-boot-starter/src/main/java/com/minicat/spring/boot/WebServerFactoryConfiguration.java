package com.minicat.spring.boot;

import com.minicat.MiniCat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Servlet;

@Configuration
public class WebServerFactoryConfiguration {

    @Bean
    @ConditionalOnClass({ Servlet.class, MiniCat.class})
    MiniCatWebServerFactory miniCatWebServerFactory() {
        MiniCatWebServerFactory factory = new MiniCatWebServerFactory();
        return factory;
    }

}