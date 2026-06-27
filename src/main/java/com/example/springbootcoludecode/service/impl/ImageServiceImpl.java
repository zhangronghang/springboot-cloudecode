package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.example.springbootcoludecode.service.ImageService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ImageServiceImpl implements ImageService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Override
    public ApiResponse upload(MultipartFile file, String title, String description, String tags, String uploader) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "图片文件不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            return ApiResponse.error(400, "标题不能为空");
        }
        try {
            ObjectId gridFsFileId = gridFsTemplate.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );

            ImageMetadata meta = new ImageMetadata();
            meta.setTitle(title.trim());
            meta.setDescription(description != null ? description.trim() : "");
            meta.setTags(tags != null ? tags.trim() : "");
            meta.setUploader(uploader != null ? uploader.trim() : "");
            meta.setUploadTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            meta.setFileSize(String.valueOf(file.getSize()));
            meta.setFileName(file.getOriginalFilename());
            meta.setGridFsFileId(gridFsFileId.toString());
            meta.setId(new ObjectId().toString());

            mongoTemplate.save(meta);

            return ApiResponse.success(meta);
        } catch (IOException e) {
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse list(ImageListRequest request) {
        return ApiResponse.error(500, "未实现");
    }

    @Override
    public ApiResponse detail(ImageDetailRequest request) {
        return ApiResponse.error(500, "未实现");
    }

    @Override
    public ApiResponse update(ImageUpdateRequest request) {
        return ApiResponse.error(500, "未实现");
    }

    @Override
    public ApiResponse delete(ImageDeleteRequest request) {
        return ApiResponse.error(500, "未实现");
    }
}
