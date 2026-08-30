package com.example.springbootcoludecode.controller;

import com.example.springbootcoludecode.dto.ApiResponse;
import com.example.springbootcoludecode.dto.ImageResource;
import com.example.springbootcoludecode.service.ImageService;
import com.example.springbootcoludecode.service.InformationImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class ImageControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageService imageService;

    @MockBean
    private InformationImageService informationImageService;

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
                        .param("cityCode", "1101")
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
    void allowsMissingAdministrativeCodesWhenUploadingInformation() throws Exception {
        when(imageService.upload(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ApiResponse.success(null));
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/information/upload")
                        .file(file)
                        .param("title", "测试图片"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void allowsPartialOrBlankAdministrativeCodesWhenUploadingInformation() throws Exception {
        when(imageService.upload(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ApiResponse.success(null));
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/information/upload")
                        .file(file)
                        .param("title", "测试图片")
                        .param("districtCode", "district-only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(multipart("/api/information/upload")
                        .file(file)
                        .param("title", "测试图片")
                        .param("provinceCode", "   ")
                        .param("cityCode", "city-only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void allowsMissingOrBlankProvinceCodeWhenUploadingInformation() throws Exception {
        when(imageService.upload(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ApiResponse.success(null));
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/information/upload")
                        .file(file)
                        .param("title", "测试图片")
                        .param("cityCode", "1101")
                        .param("districtCode", "110101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(multipart("/api/information/upload")
                        .file(file)
                        .param("title", "测试图片")
                        .param("provinceCode", "   ")
                        .param("cityCode", "1101")
                        .param("districtCode", "110101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void requiresImageFileWhenUploadingInformation() throws Exception {
        mockMvc.perform(multipart("/api/information/upload")
                        .param("title", "测试图片")
                        .param("provinceCode", "11")
                        .param("cityCode", "1101")
                        .param("districtCode", "1101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsMultipleFilesWhenCreatingInformation() throws Exception {
        MockMultipartFile first = new MockMultipartFile("file", "first.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile second = new MockMultipartFile("file", "second.jpg", "image/jpeg", new byte[]{2});

        mockMvc.perform(multipart("/api/information/upload")
                        .file(first)
                        .file(second)
                        .param("title", "测试图片"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void exposesDedicatedFootprintImageManagementAndResourceEndpoints() throws Exception {
        when(informationImageService.addImage(anyString(), any())).thenReturn(ApiResponse.success(null));
        when(informationImageService.deleteImages(any())).thenReturn(ApiResponse.success(null));
        when(informationImageService.listImages(any())).thenReturn(ApiResponse.success(null));
        ImageResource resource = new ImageResource(new byte[]{1}, "image/jpeg");
        when(informationImageService.readThumbnail(anyString(), anyString())).thenReturn(resource);
        when(informationImageService.readOriginal(anyString(), anyString())).thenReturn(resource);
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/information/image/add")
                        .file(file)
                        .param("informationId", "record-1"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/information/image/delete")
                        .contentType("application/json")
                        .content("{\"informationId\":\"record-1\",\"imageIds\":[\"image-1\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/information/image/list")
                        .contentType("application/json")
                        .content("{\"informationId\":\"record-1\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/information/record-1/images/image-1/thumbnail"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/information/record-1/images/image-1/original"))
                .andExpect(status().isOk());
    }

    @Test
    void servesBinaryImagesWithDedicatedHttpErrorsAndPermanentCacheHeaders() throws Exception {
        when(informationImageService.readOriginal("record-1", "image-1"))
                .thenReturn(new ImageResource(new byte[]{7, 8}, "image/png"));
        when(informationImageService.readThumbnail("invalid", "image-1"))
                .thenThrow(new IllegalArgumentException("图片地址参数错误"));
        when(informationImageService.readOriginal("broken", "image-1"))
                .thenThrow(new IllegalStateException("gridfs unavailable"));

        mockMvc.perform(get("/api/information/record-1/images/image-1/original"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{7, 8}))
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", "inline"))
                .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mockMvc.perform(get("/api/information/invalid/images/image-1/thumbnail"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/information/broken/images/image-1/original"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));
    }
}
