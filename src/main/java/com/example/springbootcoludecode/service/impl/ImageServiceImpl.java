package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.entity.ImageItem;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.example.springbootcoludecode.service.ImageService;
import com.example.springbootcoludecode.service.ThumbnailService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImageServiceImpl implements ImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageServiceImpl.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private GridFsTemplate gridFsTemplate;
    @Autowired private ThumbnailService thumbnailService;

    @Override public ApiResponse upload(MultipartFile file, String title, String provinceCode, String cityCode, String districtCode, String description, String tags, String uploader) {
        if (file == null || file.isEmpty()) return ApiResponse.error(400, "图片文件不能为空");
        if (!present(title)) return ApiResponse.error(400, "标题不能为空");
        String originalId = null;
        String thumbnailId = null;
        try {
            ThumbnailService.ProcessedImage processed = thumbnailService.process(file);
            ObjectId storedOriginal = gridFsTemplate.store(new java.io.ByteArrayInputStream(processed.getOriginalBytes()), file.getOriginalFilename(), processed.getContentType());
            originalId = storedOriginal.toString();
            ObjectId storedThumbnail = gridFsTemplate.store(new java.io.ByteArrayInputStream(processed.getThumbnailBytes()), "thumbnail-" + file.getOriginalFilename(), processed.getContentType());
            thumbnailId = storedThumbnail.toString();
            String now = now();
            ImageItem item = new ImageItem(); item.setImageId(new ObjectId().toString()); item.setOriginalGridFsFileId(originalId); item.setThumbnailGridFsFileId(thumbnailId);
            item.setFileName(file.getOriginalFilename()); item.setFileSize(file.getSize()); item.setContentType(processed.getContentType()); item.setWidth(processed.getWidth()); item.setHeight(processed.getHeight()); item.setCreateTime(now);
            ImageMetadata metadata = metadata(title, provinceCode, cityCode, districtCode, description, tags, uploader, now);
            metadata.getImages().add(item); mongoTemplate.save(metadata);
            return ApiResponse.success(InformationResponseMapper.toInformation(metadata));
        } catch (IllegalArgumentException e) {
            deleteQuietly(thumbnailId); deleteQuietly(originalId); return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            deleteQuietly(thumbnailId); deleteQuietly(originalId); return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    @Override public ApiResponse list(ImageListRequest request) {
        int page = request.getPage() > 0 ? request.getPage() : 1, size = request.getSize() > 0 ? request.getSize() : 10;
        Query query = filters(request); long total = mongoTemplate.count(query, ImageMetadata.class);
        query.with(PageRequest.of(page - 1, size)).with(Sort.by(Sort.Direction.DESC, "uploadTime"));
        java.util.List<InformationResponse> records = new java.util.ArrayList<>(); for (ImageMetadata metadata : mongoTemplate.find(query, ImageMetadata.class)) records.add(InformationResponseMapper.toInformation(metadata));
        Map<String, Object> data = new HashMap<>(); data.put("total", total); data.put("page", page); data.put("size", size); data.put("records", records);
        return ApiResponse.success(data);
    }
    @Override public ApiResponse detail(ImageDetailRequest request) {
        if (!present(request.getId())) return ApiResponse.error(400, "ID 不能为空");
        ImageMetadata metadata = mongoTemplate.findById(request.getId().trim(), ImageMetadata.class);
        return metadata == null ? ApiResponse.error(400, "记录不存在") : ApiResponse.success(InformationResponseMapper.toInformation(metadata));
    }
    @Override public ApiResponse update(String id, String title, String description, String tags, String uploader) {
        if (!present(id)) return ApiResponse.error(400, "ID 不能为空");
        ImageMetadata metadata = mongoTemplate.findById(id.trim(), ImageMetadata.class);
        if (metadata == null) return ApiResponse.error(400, "记录不存在");
        boolean updated = false;
        if (present(title)) { metadata.setTitle(title.trim()); updated = true; }
        if (present(description)) { metadata.setDescription(description.trim()); updated = true; }
        if (present(tags)) { metadata.setTags(tags.trim()); updated = true; }
        if (present(uploader)) { metadata.setUploader(uploader.trim()); updated = true; }
        if (updated) metadata.setUploadTime(now()); mongoTemplate.save(metadata); return ApiResponse.success(InformationResponseMapper.toInformation(metadata));
    }
    @Override public ApiResponse delete(ImageDeleteRequest request) {
        if (!present(request.getId())) return ApiResponse.error(400, "ID 不能为空");
        ImageMetadata metadata = mongoTemplate.findById(request.getId().trim(), ImageMetadata.class);
        if (metadata == null) return ApiResponse.error(400, "记录不存在");
        mongoTemplate.remove(metadata);
        if (metadata.getImages() != null) for (ImageItem item : metadata.getImages()) { deleteQuietly(item.getOriginalGridFsFileId()); deleteQuietly(item.getThumbnailGridFsFileId()); }
        return ApiResponse.success(null);
    }
    private ImageMetadata metadata(String title, String province, String city, String district, String description, String tags, String uploader, String now) {
        ImageMetadata m = new ImageMetadata(); m.setId(new ObjectId().toString()); m.setTitle(title.trim()); m.setDescription(trim(description)); m.setTags(trim(tags)); m.setUploader(trim(uploader)); m.setProvinceCode(trim(province)); m.setCityCode(trim(city)); m.setDistrictCode(trim(district)); m.setCreateTime(now); m.setUploadTime(now); return m;
    }
    private Query filters(ImageListRequest r) { Query q = new Query(); if (present(r.getTag())) q.addCriteria(Criteria.where("tags").regex(r.getTag().trim())); if (present(r.getUploader())) q.addCriteria(Criteria.where("uploader").is(r.getUploader().trim())); if (present(r.getProvinceCode())) q.addCriteria(Criteria.where("provinceCode").is(r.getProvinceCode().trim())); if (present(r.getCityCode())) q.addCriteria(Criteria.where("cityCode").is(r.getCityCode().trim())); if (present(r.getDistrictCode())) q.addCriteria(Criteria.where("districtCode").is(r.getDistrictCode().trim())); return q; }
    private void deleteQuietly(String id) { if (id != null && ObjectId.isValid(id)) try { gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(id)))); } catch (Exception e) { LOGGER.warn("GridFS 文件清理失败，文件 ID: {}", id, e); } }
    private boolean present(String value) { return value != null && !value.trim().isEmpty(); }
    private String trim(String value) { return value == null ? "" : value.trim(); }
    private String now() { return LocalDateTime.now().format(TIME_FORMATTER); }
}
