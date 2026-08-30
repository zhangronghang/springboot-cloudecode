package com.example.springbootcoludecode.controller;

import com.example.springbootcoludecode.dto.ImageResource;
import com.example.springbootcoludecode.service.InformationImageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "足迹图片资源")
public class ImageResourceController {

    @Autowired
    private InformationImageService informationImageService;

    @GetMapping(value = "/api/information/{informationId}/images/{imageId}/thumbnail", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    @ApiOperation(value = "访问足迹图片缩略图", notes = "通过公开、永久相对 URL 返回 JPEG 或 PNG 二进制缩略图，不使用 ApiResponse 包装；响应包含长期缓存和 nosniff 安全头。")
    public ResponseEntity<byte[]> thumbnail(@PathVariable String informationId, @PathVariable String imageId) {
        return read(() -> informationImageService.readThumbnail(informationId, imageId));
    }

    @GetMapping(value = "/api/information/{informationId}/images/{imageId}/original", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    @ApiOperation(value = "访问足迹图片原图", notes = "通过公开、永久相对 URL 返回 JPEG 或 PNG 二进制原图，不使用 ApiResponse 包装；响应为 inline 并包含长期缓存和 nosniff 安全头。")
    public ResponseEntity<byte[]> original(@PathVariable String informationId, @PathVariable String imageId) {
        return read(() -> informationImageService.readOriginal(informationId, imageId));
    }

    private ResponseEntity<byte[]> read(ResourceReader reader) {
        try { return response(reader.read()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().build(); }
        catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    private ResponseEntity<byte[]> response(ImageResource resource) {
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resource.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource.getBytes());
    }

    private interface ResourceReader { ImageResource read(); }
}
