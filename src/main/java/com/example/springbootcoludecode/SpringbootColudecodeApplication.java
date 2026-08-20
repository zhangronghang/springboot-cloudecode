package com.example.springbootcoludecode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class SpringbootColudecodeApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringbootColudecodeApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringbootColudecodeApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSwaggerUiAddress() {
        LOGGER.info("Swagger UI: http://localhost:8081/swagger-ui/index.html");
    }
}
