package com.nagarro.ragchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RagChatStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagChatStorageApplication.class, args);
    }
}
