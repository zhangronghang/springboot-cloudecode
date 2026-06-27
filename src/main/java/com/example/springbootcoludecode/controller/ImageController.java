package com.example.springbootcoludecode.controller;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    public ApiResponse upload(@RequestParam("file") MultipartFile file,
                              @RequestParam("title") String title,
                              @RequestParam(value = "description", required = false) String description,
                              @RequestParam(value = "tags", required = false) String tags,
                              @RequestParam(value = "uploader", required = false) String uploader) {
        return imageService.upload(file, title, description, tags, uploader);
    }

    @PostMapping("/list")
    public ApiResponse list(@RequestBody ImageListRequest request) {
        return imageService.list(request);
    }

    @PostMapping("/detail")
    public ApiResponse detail(@RequestBody ImageDetailRequest request) {
        return imageService.detail(request);
    }

    @PostMapping("/update")
    public ApiResponse update(@RequestBody ImageUpdateRequest request) {
        return imageService.update(request);
    }

    @PostMapping("/delete")
    public ApiResponse delete(@RequestBody ImageDeleteRequest request) {
        return imageService.delete(request);
    }
}
