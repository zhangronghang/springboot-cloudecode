package com.example.springbootcoludecode.dto;

import com.example.springbootcoludecode.entity.ImageItem;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InformationResponseMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsOnlyPublicCoverAndImageFields() throws Exception {
        ImageMetadata metadata = new ImageMetadata(); metadata.setId("information-1"); metadata.setTitle("足迹");
        ImageItem image = new ImageItem(); image.setImageId("image-1"); image.setOriginalGridFsFileId("internal-original"); image.setThumbnailGridFsFileId("internal-thumbnail"); image.setFileName("a.png"); image.setFileSize(12L); image.setContentType("image/png"); image.setWidth(100); image.setHeight(50); image.setCreateTime("20260828150000");
        metadata.getImages().add(image);

        String json = objectMapper.writeValueAsString(InformationResponseMapper.toInformation(metadata));
        assertTrue(json.contains("\"imageCount\":1"));
        assertTrue(json.contains("\"thumbnailUrl\":\"/api/information/information-1/images/image-1/thumbnail\""));
        assertTrue(json.contains("\"originalUrl\":\"/api/information/information-1/images/image-1/original\""));
        assertFalse(json.contains("internal-original"));
        assertFalse(json.contains("internal-thumbnail"));
        assertFalse(json.contains("\"images\""));
        assertFalse(json.contains("imageBase64"));
    }

    @Test
    void mapsEmptyImagesToZeroCountAndNullCover() throws Exception {
        ImageMetadata metadata = new ImageMetadata(); metadata.setId("information-1");
        String json = objectMapper.writeValueAsString(InformationResponseMapper.toInformation(metadata));
        assertTrue(json.contains("\"imageCount\":0"));
        assertTrue(json.contains("\"coverImage\":null"));
    }
}
