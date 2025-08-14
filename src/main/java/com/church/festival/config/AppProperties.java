package com.church.festival.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties configuration class
 */
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    
    private Upload upload = new Upload();
    private DefaultAdmin defaultAdmin = new DefaultAdmin();
    
    @Data
    public static class Upload {
        private String dir = "./uploads/";
    }
    
    @Data
    public static class DefaultAdmin {
        private String username = "admin";
        private String password = "admin123";
        private String email = "admin@church.com";
    }
}
