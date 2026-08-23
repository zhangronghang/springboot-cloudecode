package com.example.springbootcoludecode.controller;

import com.example.springbootcoludecode.dto.ApiResponse;
import com.example.springbootcoludecode.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
class ImageControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageService imageService;

    @Test
    void exposesAllInformationManagementEndpointsAndRemovesLegacyImageEndpoints() throws Exception {
        when(imageService.list(any())).thenReturn(ApiResponse.success(null));
        when(imageService.detail(any())).thenReturn(ApiResponse.success(null));
        when(imageService.delete(any())).thenReturn(ApiResponse.success(null));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/api/information/upload")
                        .file(file)
                        .param("title", "测试图片")
                        .param("provinceCode", "11")
                        .param("districtCode", "1101"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/information/list")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/information/detail")
                        .contentType("application/json")
                        .content("{\"id\":\"record-1\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/information/update")
                        .param("id", "record-1")
                        .param("title", "更新标题"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/information/delete")
                        .contentType("application/json")
                        .content("{\"id\":\"record-1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/images/list")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresBothAdministrativeCodesWhenUploadingInformation() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/information/upload")
                .file(file)
                .param("title", "测试图片")
                .param("districtCode", "1101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(multipart("/api/information/upload")
                .file(file)
                .param("title", "测试图片")
                .param("provinceCode", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void requiresImageFileWhenUploadingInformation() throws Exception {
        mockMvc.perform(multipart("/api/information/upload")
                        .param("title", "测试图片")
                        .param("provinceCode", "11")
                        .param("districtCode", "1101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
