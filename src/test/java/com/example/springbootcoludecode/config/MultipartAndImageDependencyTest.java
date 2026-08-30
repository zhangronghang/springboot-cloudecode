package com.example.springbootcoludecode.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MultipartAndImageDependencyTest {

    @Test
    void configuresFiftyMegabyteFileAndFiftyFiveMegabyteRequestLimits() throws Exception {
        try (InputStream input = new ClassPathResource("application.yaml").getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = new Yaml().load(input);
            @SuppressWarnings("unchecked")
            Map<String, Object> spring = (Map<String, Object>) root.get("spring");
            @SuppressWarnings("unchecked")
            Map<String, Object> servlet = (Map<String, Object>) spring.get("servlet");
            @SuppressWarnings("unchecked")
            Map<String, Object> multipart = (Map<String, Object>) servlet.get("multipart");

            assertEquals("50MB", multipart.get("max-file-size"));
            assertEquals("55MB", multipart.get("max-request-size"));
        }
    }

    @Test
    void providesJavaEightCompatibleExifMetadataReader() throws Exception {
        assertNotNull(Class.forName("com.drew.imaging.ImageMetadataReader"));
    }
}
