package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.ApiResponse;
import com.example.springbootcoludecode.dto.ImageListRequest;
import com.example.springbootcoludecode.dto.ImageDetailRequest;
import com.example.springbootcoludecode.dto.InformationResponse;
import com.example.springbootcoludecode.entity.ImageItem;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.example.springbootcoludecode.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ImageQueryContractTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private GridFsTemplate gridFsTemplate;
    @InjectMocks private ImageServiceImpl service;

    @BeforeEach void setUp() { MockitoAnnotations.initMocks(this); }

    @Test
    void listMapsOnlyTheFirstImageAsPublicCoverWithoutReadingGridFs() {
        ImageMetadata metadata = metadataWithImage();
        when(mongoTemplate.count(any(Query.class), eq(ImageMetadata.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(Collections.singletonList(metadata));
        ApiResponse response = service.list(new ImageListRequest());
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) response.getData();
        InformationResponse record = (InformationResponse) ((java.util.List<?>) data.get("records")).get(0);
        assertEquals(1, record.getImageCount());
        assertEquals("image-1", record.getCoverImage().getImageId());
        assertTrue(record.getCoverImage().getThumbnailUrl().endsWith("/thumbnail"));
        verifyNoInteractions(gridFsTemplate);
    }

    @Test
    void listRepresentsNoImagesWithZeroCountAndNullCover() {
        ImageMetadata metadata = new ImageMetadata(); metadata.setId("record");
        when(mongoTemplate.count(any(Query.class), eq(ImageMetadata.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(ImageMetadata.class))).thenReturn(Collections.singletonList(metadata));
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) service.list(new ImageListRequest()).getData();
        InformationResponse record = (InformationResponse) ((java.util.List<?>) data.get("records")).get(0);
        assertEquals(0, record.getImageCount()); assertNull(record.getCoverImage()); verifyNoInteractions(gridFsTemplate);
    }

    @Test
    void updateServiceContractDoesNotAcceptAMultipartFile() {
        assertTrue(Arrays.stream(ImageService.class.getMethods())
                .filter(method -> method.getName().equals("update"))
                .anyMatch(method -> method.getParameterCount() == 5));
    }

    @Test
    void detailReturnsOnlyPublicMetadataWithoutReadingGridFs() {
        ImageMetadata metadata = metadataWithImage();
        when(mongoTemplate.findById("record", ImageMetadata.class)).thenReturn(metadata);
        ImageDetailRequest request = new ImageDetailRequest(); request.setId("record");
        ApiResponse response = service.detail(request);
        assertTrue(response.getData() instanceof InformationResponse);
        InformationResponse detail = (InformationResponse) response.getData();
        assertEquals(1, detail.getImageCount()); assertEquals("image-1", detail.getCoverImage().getImageId());
        verifyNoInteractions(gridFsTemplate);
    }

    private ImageMetadata metadataWithImage() {
        ImageMetadata metadata = new ImageMetadata(); metadata.setId("record");
        ImageItem image = new ImageItem(); image.setImageId("image-1"); image.setOriginalGridFsFileId("internal"); image.setThumbnailGridFsFileId("internal-thumb"); image.setContentType("image/jpeg"); metadata.getImages().add(image); return metadata;
    }
}
