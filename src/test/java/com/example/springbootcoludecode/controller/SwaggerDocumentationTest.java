package com.example.springbootcoludecode.controller;

import com.example.springbootcoludecode.config.SwaggerConfig;
import com.example.springbootcoludecode.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
@Import(SwaggerConfig.class)
class SwaggerDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageService imageService;

    @Test
    void exposesImageApiDefinitionForFrontendIntegration() throws Exception {
        mockMvc.perform(get("/v2/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/information/upload']").exists())
                .andExpect(jsonPath("$.paths['/api/information/list']").exists())
                .andExpect(jsonPath("$.paths['/api/information/detail']").exists())
                .andExpect(jsonPath("$.paths['/api/information/update']").exists())
                .andExpect(jsonPath("$.paths['/api/information/delete']").exists())
                .andExpect(jsonPath("$.paths['/api/images/upload']").doesNotExist())
                .andExpect(jsonPath("$.tags[?(@.name == '信息管理')]").exists())
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'provinceCode')].required").value(hasItem(true)))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'provinceCode')].description").value(hasItem("省级行政区划代码")))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'districtCode')].required").value(hasItem(true)))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'districtCode')].description").value(hasItem("区县级行政区划代码")));
    }
}
