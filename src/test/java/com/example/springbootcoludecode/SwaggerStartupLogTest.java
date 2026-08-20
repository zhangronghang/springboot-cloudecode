package com.example.springbootcoludecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SwaggerStartupLogTest {

    @Test
    void logsSwaggerUiAddressAfterApplicationStartup(CapturedOutput output) {
        assertThat(output).contains("Swagger UI: http://localhost:8081/swagger-ui/index.html");
    }
}
