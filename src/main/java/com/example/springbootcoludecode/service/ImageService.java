package com.example.springbootcoludecode.service;

import com.example.springbootcoludecode.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    ApiResponse upload(MultipartFile file, String title, String provinceCode, String cityCode, String districtCode, String description, String tags, String uploader);
    ApiResponse list(ImageListRequest request);
    ApiResponse detail(ImageDetailRequest request);
    ApiResponse update(String id, MultipartFile file, String title, String description, String tags, String uploader);
    ApiResponse delete(ImageDeleteRequest request);
}
