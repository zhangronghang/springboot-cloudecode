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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
}
