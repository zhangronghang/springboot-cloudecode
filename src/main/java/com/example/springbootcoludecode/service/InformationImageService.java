package com.example.springbootcoludecode.service;

import com.example.springbootcoludecode.dto.ApiResponse;
import com.example.springbootcoludecode.dto.ImageBatchDeleteRequest;
import com.example.springbootcoludecode.dto.ImagePageRequest;
import com.example.springbootcoludecode.dto.ImageResource;
import org.springframework.web.multipart.MultipartFile;

public interface InformationImageService {
    ApiResponse addImage(String informationId, MultipartFile file);
    ApiResponse deleteImages(ImageBatchDeleteRequest request);
    ApiResponse listImages(ImagePageRequest request);
    ImageResource readThumbnail(String informationId, String imageId);
    ImageResource readOriginal(String informationId, String imageId);
}
