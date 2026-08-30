package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.ApiResponse;
import com.example.springbootcoludecode.dto.InformationResponse;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.example.springbootcoludecode.service.ThumbnailService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageServiceImplTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private GridFsTemplate gridFsTemplate;
    @Mock private ThumbnailService thumbnailService;
    @InjectMocks private ImageServiceImpl imageService;
    private MockMultipartFile file;

    @BeforeEach void setUp() { MockitoAnnotations.initMocks(this); file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2}); }

    @Test
    void uploadStoresOriginalAndThumbnailAsFirstImageWithOneTimestamp() throws Exception {
        when(thumbnailService.process(file)).thenReturn(new ThumbnailService.ProcessedImage(new byte[]{1, 2}, new byte[]{3}, "image/jpeg", 640, 480));
        ObjectId original = new ObjectId(), thumbnail = new ObjectId(); when(gridFsTemplate.store(any(), anyString(), anyString())).thenReturn(original, thumbnail);
        ApiResponse response = imageService.upload(file, " 标题 ", " ", " city ", null, "描述", "标签", "上传者");
        assertEquals(200, response.getCode()); assertTrue(response.getData() instanceof InformationResponse);
        ArgumentCaptor<ImageMetadata> captor = ArgumentCaptor.forClass(ImageMetadata.class); verify(mongoTemplate).save(captor.capture());
        ImageMetadata metadata = captor.getValue(); assertEquals(1, metadata.getImages().size());
        assertEquals(original.toString(), metadata.getImages().get(0).getOriginalGridFsFileId()); assertEquals(thumbnail.toString(), metadata.getImages().get(0).getThumbnailGridFsFileId());
        assertEquals(640, metadata.getImages().get(0).getWidth()); assertEquals(480, metadata.getImages().get(0).getHeight()); assertEquals("", metadata.getProvinceCode()); assertEquals("city", metadata.getCityCode()); assertEquals(metadata.getCreateTime(), metadata.getUploadTime());
    }

    @Test
    void uploadCompensatesOriginalAndThumbnailWhenMetadataSaveFails() throws Exception {
        when(thumbnailService.process(file)).thenReturn(new ThumbnailService.ProcessedImage(new byte[]{1}, new byte[]{2}, "image/jpeg", 1, 1));
        when(gridFsTemplate.store(any(), anyString(), anyString())).thenReturn(new ObjectId(), new ObjectId()); doThrow(new RuntimeException("mongo down")).when(mongoTemplate).save(any(ImageMetadata.class));
        ApiResponse response = imageService.upload(file, "标题", null, null, null, null, null, null);
        assertEquals(500, response.getCode()); verify(gridFsTemplate, times(2)).delete(any(Query.class));
    }

    @Test
    void uploadCompensatesOriginalWhenThumbnailStorageFails() throws Exception {
        when(thumbnailService.process(file)).thenReturn(new ThumbnailService.ProcessedImage(new byte[]{1}, new byte[]{2}, "image/jpeg", 1, 1));
        when(gridFsTemplate.store(any(), anyString(), anyString())).thenReturn(new ObjectId()).thenThrow(new RuntimeException("gridfs down"));
        ApiResponse response = imageService.upload(file, "标题", null, null, null, null, null, null);
        assertEquals(500, response.getCode()); verify(gridFsTemplate).delete(any(Query.class)); verify(mongoTemplate, never()).save(any(ImageMetadata.class));
    }

    @Test
    void deletesMetadataBeforeAllImageFilesAndLogsCleanupFailureWithoutRollback() {
        ImageMetadata metadata = new ImageMetadata(); metadata.setId("record");
        com.example.springbootcoludecode.entity.ImageItem first = image();
        com.example.springbootcoludecode.entity.ImageItem second = image();
        metadata.getImages().add(first); metadata.getImages().add(second);
        when(mongoTemplate.findById("record", ImageMetadata.class)).thenReturn(metadata);
        doThrow(new RuntimeException("gridfs down")).when(gridFsTemplate).delete(any(Query.class));
        Logger logger = (Logger) LoggerFactory.getLogger(ImageServiceImpl.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>(); events.start(); logger.addAppender(events);

        try {
            com.example.springbootcoludecode.dto.ImageDeleteRequest request = new com.example.springbootcoludecode.dto.ImageDeleteRequest();
            request.setId("record");
            ApiResponse response = imageService.delete(request);
            assertEquals(200, response.getCode());
            org.mockito.InOrder ordered = inOrder(mongoTemplate, gridFsTemplate);
            ordered.verify(mongoTemplate).remove(metadata);
            ordered.verify(gridFsTemplate, times(4)).delete(any(Query.class));
            assertTrue(events.list.stream().anyMatch(event -> event.getFormattedMessage().contains("GridFS")));
        } finally {
            logger.detachAppender(events);
        }
    }

    private com.example.springbootcoludecode.entity.ImageItem image() {
        com.example.springbootcoludecode.entity.ImageItem item = new com.example.springbootcoludecode.entity.ImageItem();
        item.setOriginalGridFsFileId(new ObjectId().toString()); item.setThumbnailGridFsFileId(new ObjectId().toString());
        return item;
    }
}
