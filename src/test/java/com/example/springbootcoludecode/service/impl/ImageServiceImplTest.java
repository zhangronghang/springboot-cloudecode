package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.*;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ImageServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @InjectMocks
    private ImageServiceImpl imageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void upload_shouldStoreFileAndSaveMetadata() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));

        org.bson.types.ObjectId gridFsId = new org.bson.types.ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString())).thenReturn(gridFsId);

        ApiResponse response = imageService.upload(file, "测试标题", "测试描述", "风景,旅游", "zhangsan");

        assertEquals(200, response.getCode());
        assertTrue(response.getData() instanceof ImageMetadata);
        ImageMetadata meta = (ImageMetadata) response.getData();
        assertEquals("测试标题", meta.getTitle());
        assertEquals("测试描述", meta.getDescription());
        assertEquals("风景,旅游", meta.getTags());
        assertEquals("zhangsan", meta.getUploader());
        assertEquals("1024", meta.getFileSize());
        assertEquals("test.jpg", meta.getFileName());
        assertEquals(gridFsId.toString(), meta.getGridFsFileId());
        assertNotNull(meta.getUploadTime());
        assertNotNull(meta.getId());

        verify(mongoTemplate).save(any(ImageMetadata.class));
    }

    @Test
    void list_shouldReturnPaginatedResults() {
        ImageListRequest request = new ImageListRequest();
        request.setPage(1);
        request.setSize(10);

        ImageMetadata meta = new ImageMetadata();
        meta.setId("507f1f77bcf86cd799439011");
        meta.setTitle("测试");
        meta.setUploader("zhangsan");
        meta.setUploadTime("2026-06-27 10:30:00");

        when(mongoTemplate.count(any(Query.class), eq(ImageMetadata.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(Collections.singletonList(meta));

        ApiResponse response = imageService.list(request);

        assertEquals(200, response.getCode());
        assertTrue(response.getData() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertEquals(1L, data.get("total"));
        assertEquals(1, data.get("page"));
        assertEquals(10, data.get("size"));
        assertNotNull(data.get("records"));
    }

    @Test
    void list_shouldFilterByTagAndUploader() {
        ImageListRequest request = new ImageListRequest();
        request.setPage(1);
        request.setSize(5);
        request.setTag("风景");
        request.setUploader("zhangsan");

        when(mongoTemplate.count(any(Query.class), eq(ImageMetadata.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(Collections.emptyList());

        ApiResponse response = imageService.list(request);

        assertEquals(200, response.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertEquals(0L, data.get("total"));
    }

    @Test
    void detail_shouldReturnMetadataAndBase64() throws Exception {
        ImageDetailRequest request = new ImageDetailRequest();
        request.setId("507f1f77bcf86cd799439011");

        ImageMetadata meta = new ImageMetadata();
        meta.setId("507f1f77bcf86cd799439011");
        meta.setTitle("测试图片");
        meta.setGridFsFileId(null); // 无图片文件的场景
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(meta);

        ApiResponse response = imageService.detail(request);

        assertEquals(200, response.getCode());
        assertTrue(response.getData() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertEquals("测试图片", ((ImageMetadata) data.get("metadata")).getTitle());
    }

    @Test
    void detail_shouldReturnMetadataWithNullBase64WhenGridFsFileNotFound() {
        ImageDetailRequest request = new ImageDetailRequest();
        request.setId("507f1f77bcf86cd799439011");

        ImageMetadata meta = new ImageMetadata();
        meta.setId("507f1f77bcf86cd799439011");
        meta.setTitle("测试");
        meta.setGridFsFileId("507f1f77bcf86cd799439022");
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(meta);
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(null);

        ApiResponse response = imageService.detail(request);

        assertEquals(200, response.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertNotNull(data.get("metadata"));
        assertNull(data.get("imageBase64"));
    }

    @Test
    void detail_shouldReturn404WhenNotFound() {
        ImageDetailRequest request = new ImageDetailRequest();
        request.setId("nonexistent");
        when(mongoTemplate.findById("nonexistent", ImageMetadata.class)).thenReturn(null);

        ApiResponse response = imageService.detail(request);

        assertEquals(400, response.getCode());
        assertEquals("记录不存在", response.getMessage());
    }

    @Test
    void update_shouldUpdateFields() {
        ImageUpdateRequest request = new ImageUpdateRequest();
        request.setId("507f1f77bcf86cd799439011");
        request.setTitle("新标题");
        request.setDescription("新描述");

        ImageMetadata existing = new ImageMetadata();
        existing.setId("507f1f77bcf86cd799439011");
        existing.setTitle("旧标题");
        existing.setDescription("旧描述");
        existing.setTags("旧标签");
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(existing);

        ApiResponse response = imageService.update(request);

        assertEquals(200, response.getCode());
        ImageMetadata updated = (ImageMetadata) response.getData();
        assertEquals("新标题", updated.getTitle());
        assertEquals("新描述", updated.getDescription());
        assertEquals("旧标签", updated.getTags());
        verify(mongoTemplate).save(existing);
    }

    @Test
    void update_shouldReturn404WhenNotFound() {
        ImageUpdateRequest request = new ImageUpdateRequest();
        request.setId("nonexistent");
        request.setTitle("新标题");
        when(mongoTemplate.findById("nonexistent", ImageMetadata.class)).thenReturn(null);

        ApiResponse response = imageService.update(request);

        assertEquals(400, response.getCode());
        assertEquals("记录不存在", response.getMessage());
        verify(mongoTemplate, never()).save(any());
    }

    @Test
    void delete_shouldRemoveFileAndMetadata() {
        ImageDeleteRequest request = new ImageDeleteRequest();
        request.setId("507f1f77bcf86cd799439011");

        ImageMetadata existing = new ImageMetadata();
        existing.setId("507f1f77bcf86cd799439011");
        existing.setGridFsFileId("507f1f77bcf86cd799439022");
        when(mongoTemplate.findById("507f1f77bcf86cd799439011", ImageMetadata.class)).thenReturn(existing);

        ApiResponse response = imageService.delete(request);

        assertEquals(200, response.getCode());
        verify(gridFsTemplate).delete(any(Query.class));
        verify(mongoTemplate).remove(existing);
    }

    @Test
    void delete_shouldReturn404WhenNotFound() {
        ImageDeleteRequest request = new ImageDeleteRequest();
        request.setId("nonexistent");
        when(mongoTemplate.findById("nonexistent", ImageMetadata.class)).thenReturn(null);

        ApiResponse response = imageService.delete(request);

        assertEquals(400, response.getCode());
        assertEquals("记录不存在", response.getMessage());
        verify(gridFsTemplate, never()).delete(any(Query.class));
    }
}
