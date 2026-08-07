package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.example.springbootcoludecode.service.ImageService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        int page = request.getPage() > 0 ? request.getPage() : 1;
        int size = request.getSize() > 0 ? request.getSize() : 10;

        Query query = new Query();
        if (request.getTag() != null && !request.getTag().trim().isEmpty()) {
            query.addCriteria(Criteria.where("tags").regex(request.getTag().trim()));
        }
        if (request.getUploader() != null && !request.getUploader().trim().isEmpty()) {
            query.addCriteria(Criteria.where("uploader").is(request.getUploader().trim()));
        }

        long total = mongoTemplate.count(query, ImageMetadata.class);
        query.with(PageRequest.of(page - 1, size));
        query.with(Sort.by(Sort.Direction.DESC, "uploadTime"));
        List<ImageMetadata> records = mongoTemplate.find(query, ImageMetadata.class);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("records", records);

        return ApiResponse.success(data);
    }

    @Override
    public ApiResponse detail(ImageDetailRequest request) {
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            return ApiResponse.error(400, "ID 不能为空");
        }
        ImageMetadata meta = mongoTemplate.findById(request.getId().trim(), ImageMetadata.class);
        if (meta == null) {
            return ApiResponse.error(400, "记录不存在");
        }

        String imageBase64 = null;
        if (meta.getGridFsFileId() != null) {
            try {
                ObjectId fileId = new ObjectId(meta.getGridFsFileId());
                Query gridFsQuery = new Query(Criteria.where("_id").is(fileId));
                com.mongodb.client.gridfs.model.GridFSFile gridFsFile = gridFsTemplate.findOne(gridFsQuery);
                if (gridFsFile != null) {
                    org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate.getResource(gridFsFile);
                    byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
                    imageBase64 = Base64.getEncoder().encodeToString(bytes);
                }
            } catch (Exception e) {
                // 图片读取失败不影响元数据返回
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("metadata", meta);
        data.put("imageBase64", imageBase64);
        return ApiResponse.success(data);
    }

    @Override
    public ApiResponse update(String id, MultipartFile file, String title, String description, String tags, String uploader) {
        if (id == null || id.trim().isEmpty()) {
            return ApiResponse.error(400, "ID 不能为空");
        }
        ImageMetadata meta = mongoTemplate.findById(id.trim(), ImageMetadata.class);
        if (meta == null) {
            return ApiResponse.error(400, "记录不存在");
        }

        // 传了文件则替换 GridFS 图片：先存新文件，成功后删除旧文件，避免存新失败导致图片丢失
        if (file != null && !file.isEmpty()) {
            try {
                ObjectId newFileId = gridFsTemplate.store(
                        file.getInputStream(),
                        file.getOriginalFilename(),
                        file.getContentType()
                );
                if (meta.getGridFsFileId() != null) {
                    try {
                        ObjectId oldFileId = new ObjectId(meta.getGridFsFileId());
                        gridFsTemplate.delete(new Query(Criteria.where("_id").is(oldFileId)));
                    } catch (Exception e) {
                        // 旧文件删除失败不阻断更新
                    }
                }
                meta.setGridFsFileId(newFileId.toString());
                meta.setFileName(file.getOriginalFilename());
                meta.setFileSize(String.valueOf(file.getSize()));
            } catch (IOException e) {
                return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
            }
        }

        if (title != null && !title.trim().isEmpty()) {
            meta.setTitle(title.trim());
        }
        if (description != null && !description.trim().isEmpty()) {
            meta.setDescription(description.trim());
        }
        if (tags != null && !tags.trim().isEmpty()) {
            meta.setTags(tags.trim());
        }
        if (uploader != null && !uploader.trim().isEmpty()) {
            meta.setUploader(uploader.trim());
        }
        mongoTemplate.save(meta);
        return ApiResponse.success(meta);
    }

    @Override
    public ApiResponse delete(ImageDeleteRequest request) {
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            return ApiResponse.error(400, "ID 不能为空");
        }
        ImageMetadata meta = mongoTemplate.findById(request.getId().trim(), ImageMetadata.class);
        if (meta == null) {
            return ApiResponse.error(400, "记录不存在");
        }
        if (meta.getGridFsFileId() != null) {
            try {
                ObjectId fileId = new ObjectId(meta.getGridFsFileId());
                gridFsTemplate.delete(new Query(Criteria.where("_id").is(fileId)));
            } catch (Exception e) {
                // GridFS 删除失败也继续删除元数据
            }
        }
        mongoTemplate.remove(meta);
        return ApiResponse.success(null);
    }
}
