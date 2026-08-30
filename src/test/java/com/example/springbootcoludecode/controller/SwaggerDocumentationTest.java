package com.example.springbootcoludecode.controller;

import com.example.springbootcoludecode.config.SwaggerConfig;
import com.example.springbootcoludecode.service.ImageService;
import com.example.springbootcoludecode.service.InformationImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(SwaggerConfig.class)
class SwaggerDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageService imageService;

    @MockBean
    private InformationImageService informationImageService;

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
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.description").value(containsString("provinceCode、cityCode、districtCode 均可选")))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.responses['400'].description").value("图片文件或标题为空"))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'provinceCode')].required").value(hasItem(false)))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'provinceCode')].description").value(hasItem("省级行政区划代码，可选；缺失或空白保存为空字符串")))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'cityCode')].required").value(hasItem(false)))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'cityCode')].description").value(hasItem("市级行政区划代码，可选；缺失或空白保存为空字符串")))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'districtCode')].required").value(hasItem(false)))
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.parameters[?(@.name == 'districtCode')].description").value(hasItem("区县级行政区划代码，可选；缺失或空白保存为空字符串")))
                .andExpect(jsonPath("$.paths['/api/information/list'].post.description").value(containsString("市级行政区划代码精确匹配")))
                .andExpect(jsonPath("$.definitions.ImageListRequest.properties.cityCode.description").value("市级行政区划代码，非空时精确匹配"))
                .andExpect(jsonPath("$.paths['/api/information/update'].post.description").value(containsString("三个行政区划代码不允许修改")))
                .andExpect(jsonPath("$.paths['/api/information/update'].post.parameters[?(@.name == 'provinceCode')]").isEmpty())
                .andExpect(jsonPath("$.paths['/api/information/update'].post.parameters[?(@.name == 'cityCode')]").isEmpty())
                .andExpect(jsonPath("$.paths['/api/information/update'].post.parameters[?(@.name == 'districtCode')]").isEmpty());
    }

    @Test
    void documentsFootprintImageGalleryContracts() throws Exception {
        mockMvc.perform(get("/v2/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/information/image/add']").exists())
                .andExpect(jsonPath("$.paths['/api/information/image/delete']").exists())
                .andExpect(jsonPath("$.paths['/api/information/image/list']").exists())
                .andExpect(jsonPath("$.paths['/api/information/{informationId}/images/{imageId}/thumbnail']").exists())
                .andExpect(jsonPath("$.paths['/api/information/{informationId}/images/{imageId}/original']").exists())
                .andExpect(jsonPath("$.paths['/api/information/upload'].post.description").value(containsString("只能上传一张")))
                .andExpect(jsonPath("$.paths['/api/information/update'].post.parameters[?(@.name == 'file')]").isEmpty())
                .andExpect(jsonPath("$.paths['/api/information/image/add'].post.description").value(containsString("每次只能新增一张")))
                .andExpect(jsonPath("$.paths['/api/information/image/delete'].post.description").value(containsString("一张或多张")))
                .andExpect(jsonPath("$.paths['/api/information/image/list'].post.description").value(containsString("分页")))
                .andExpect(jsonPath("$.paths['/api/information/{informationId}/images/{imageId}/thumbnail'].get.produces", hasItem("image/jpeg")))
                .andExpect(jsonPath("$.paths['/api/information/{informationId}/images/{imageId}/original'].get.description").value(containsString("原图")));
    }
}
