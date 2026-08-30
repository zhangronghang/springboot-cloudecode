package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.entity.ImageItem;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.example.springbootcoludecode.service.InformationImageService;
import com.example.springbootcoludecode.service.ThumbnailService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class InformationImageServiceImpl implements InformationImageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InformationImageServiceImpl.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private GridFsTemplate gridFsTemplate;
    @Autowired private ThumbnailService thumbnailService;

    @Override public ApiResponse addImage(String informationId, MultipartFile file) {
        if (!present(informationId) || file == null || file.isEmpty()) return ApiResponse.error(400, "informationId 和图片文件不能为空");
        ImageMetadata before = mongoTemplate.findById(informationId.trim(), ImageMetadata.class);
        if (before == null) return ApiResponse.error(400, "记录不存在");
        if (before.getImages().size() >= 50) return ApiResponse.error(400, "单个足迹最多 50 张图片");
        String originalId = null, thumbnailId = null;
        boolean appended = false;
        try {
            ThumbnailService.ProcessedImage processed = thumbnailService.process(file);
            originalId = gridFsTemplate.store(new ByteArrayInputStream(processed.getOriginalBytes()), file.getOriginalFilename(), processed.getContentType()).toString();
            thumbnailId = gridFsTemplate.store(new ByteArrayInputStream(processed.getThumbnailBytes()), "thumbnail-" + file.getOriginalFilename(), processed.getContentType()).toString();
            ImageItem item = item(file, processed, originalId, thumbnailId);
            Query query = new Query(new Criteria().andOperator(Criteria.where("_id").is(informationId.trim()), Criteria.where("images.49").exists(false)));
            Update update = new Update().push("images", item).set("uploadTime", now());
            ImageMetadata updated = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), ImageMetadata.class);
            if (updated == null) {
                boolean exists = mongoTemplate.exists(new Query(Criteria.where("_id").is(informationId.trim())), ImageMetadata.class);
                return ApiResponse.error(400, exists ? "单个足迹最多 50 张图片" : "记录不存在");
            }
            appended = true;
            return ApiResponse.success(InformationResponseMapper.toImage(informationId.trim(), item));
        } catch (IllegalArgumentException e) { return ApiResponse.error(400, e.getMessage()); }
        catch (Exception e) { return ApiResponse.error(500, "图片新增失败: " + e.getMessage()); }
        finally { if (!appended) { deleteQuietly(thumbnailId); deleteQuietly(originalId); } }
    }
    private ImageItem item(MultipartFile file, ThumbnailService.ProcessedImage processed, String originalId, String thumbnailId) {
        ImageItem item = new ImageItem(); item.setImageId(new ObjectId().toString()); item.setOriginalGridFsFileId(originalId); item.setThumbnailGridFsFileId(thumbnailId); item.setFileName(file.getOriginalFilename()); item.setFileSize(file.getSize()); item.setContentType(processed.getContentType()); item.setWidth(processed.getWidth()); item.setHeight(processed.getHeight()); item.setCreateTime(now()); return item;
    }
    @Override public ApiResponse deleteImages(ImageBatchDeleteRequest request) {
        if (request == null || !present(request.getInformationId()) || request.getImageIds() == null) return ApiResponse.error(400, "informationId 和 imageIds 不能为空");
        List<String> ids = new ArrayList<>(); for (String id : new LinkedHashSet<>(request.getImageIds())) if (present(id)) ids.add(id);
        if (ids.isEmpty()) return ApiResponse.error(400, "imageIds 不能为空");
        ImageMetadata before = mongoTemplate.findById(request.getInformationId().trim(), ImageMetadata.class);
        if (before == null) return ApiResponse.error(400, "记录不存在");
        List<ImageItem> removed = new ArrayList<>(); for (ImageItem item : before.getImages()) if (ids.contains(item.getImageId())) removed.add(item);
        List<String> ignored = new ArrayList<>(); for (String id : ids) { boolean found = false; for (ImageItem item : removed) if (id.equals(item.getImageId())) found = true; if (!found) ignored.add(id); }
        if (!removed.isEmpty()) {
            Query query = new Query(new Criteria().andOperator(Criteria.where("_id").is(request.getInformationId().trim()), Criteria.where("images.imageId").in(ids)));
            Update update = new Update().pull("images", new Query(Criteria.where("imageId").in(ids)).getQueryObject()).set("uploadTime", now());
            ImageMetadata changed = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), ImageMetadata.class);
            if (changed == null) { ignored = ids; removed.clear(); }
        }
        for (ImageItem item : removed) { deleteQuietly(item.getOriginalGridFsFileId()); deleteQuietly(item.getThumbnailGridFsFileId()); }
        ImageMetadata after = mongoTemplate.findById(request.getInformationId().trim(), ImageMetadata.class);
        int remaining = after == null || after.getImages() == null ? Math.max(0, before.getImages().size() - removed.size()) : after.getImages().size();
        Map<String, Object> data = new HashMap<>(); data.put("requestedCount", ids.size()); data.put("deletedCount", removed.size()); data.put("ignoredImageIds", ignored); data.put("remainingCount", remaining); return ApiResponse.success(data);
    }
    @Override public ApiResponse listImages(ImagePageRequest request) {
        if (request == null || !present(request.getInformationId())) return ApiResponse.error(400, "informationId 不能为空");
        ImageMetadata metadata = mongoTemplate.findById(request.getInformationId().trim(), ImageMetadata.class); if (metadata == null) return ApiResponse.error(400, "记录不存在");
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage(), size = request.getSize() == null || request.getSize() < 1 ? 10 : request.getSize();
        List<ImageItem> images = metadata.getImages() == null ? new ArrayList<ImageItem>() : metadata.getImages(); int from = Math.min((page - 1) * size, images.size()), to = Math.min(from + size, images.size()); List<PublicImageResponse> records = new ArrayList<>(); for (ImageItem item : images.subList(from, to)) records.add(InformationResponseMapper.toImage(metadata.getId(), item));
        Map<String, Object> data = new HashMap<>(); data.put("total", images.size()); data.put("page", page); data.put("size", size); data.put("records", records); return images.isEmpty() ? new ApiResponse(200, "该足迹没有图片", data) : ApiResponse.success(data);
    }
    @Override public ImageResource readThumbnail(String informationId, String imageId) { return read(informationId, imageId, true); }
    @Override public ImageResource readOriginal(String informationId, String imageId) { return read(informationId, imageId, false); }
    private ImageResource read(String informationId, String imageId, boolean thumbnail) {
        if (!present(informationId) || !present(imageId)) throw new IllegalArgumentException("图片地址参数错误");
        try { ImageMetadata metadata = mongoTemplate.findById(informationId, ImageMetadata.class); if (metadata == null) return null; ImageItem found = null; for (ImageItem item : metadata.getImages()) if (imageId.equals(item.getImageId())) { found = item; break; } if (found == null) return null; String fileId = thumbnail ? found.getThumbnailGridFsFileId() : found.getOriginalGridFsFileId(); if (!ObjectId.isValid(fileId)) return null; com.mongodb.client.gridfs.model.GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId)))); if (file == null) return null; org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate.getResource(file); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream(); InputStream input = resource.getInputStream(); byte[] buffer = new byte[8192]; int count; while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count); input.close(); return new ImageResource(output.toByteArray(), found.getContentType()); } catch (IllegalArgumentException e) { throw e; } catch (Exception e) { throw new IllegalStateException("图片资源读取失败", e); }
    }
    private boolean present(String value) { return value != null && !value.trim().isEmpty(); }
    private String now() { return LocalDateTime.now().format(TIME); }
    private void deleteQuietly(String id) { if (id != null && ObjectId.isValid(id)) try { gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(id)))); } catch (Exception e) { LOGGER.warn("GridFS 文件清理失败，文件 ID: {}", id, e); } }
}
