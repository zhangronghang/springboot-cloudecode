package com.example.springbootcoludecode.service.impl;

import com.example.springbootcoludecode.dto.ApiResponse;
import com.example.springbootcoludecode.dto.ImagePageRequest;
import com.example.springbootcoludecode.dto.ImageBatchDeleteRequest;
import com.example.springbootcoludecode.entity.ImageMetadata;
import com.example.springbootcoludecode.service.ThumbnailService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InformationImageServiceImplTest {
    @Mock private MongoTemplate mongoTemplate;
    @Mock private GridFsTemplate gridFsTemplate;
    @Mock private ThumbnailService thumbnailService;
    @InjectMocks private InformationImageServiceImpl service;
    private MockMultipartFile file;

    @BeforeEach void setUp() { MockitoAnnotations.initMocks(this); file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}); }

    @Test
    void appendsOneImageWithAtomicFiftyItemGuard() {
        ImageMetadata existing = new ImageMetadata(); existing.setId("record");
        ImageMetadata updated = new ImageMetadata(); updated.setId("record");
        when(mongoTemplate.findById("record", ImageMetadata.class)).thenReturn(existing);
        when(thumbnailService.process(file)).thenReturn(new ThumbnailService.ProcessedImage(new byte[]{1}, new byte[]{2}, "image/png", 2, 3));
        when(gridFsTemplate.store(any(), anyString(), anyString())).thenReturn(new ObjectId(), new ObjectId());
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(ImageMetadata.class))).thenReturn(updated);
        ApiResponse response = service.addImage("record", file);
        assertEquals(200, response.getCode()); verify(mongoTemplate).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(ImageMetadata.class));
    }

    @Test
    void rejectsMissingOrFullFootprintBeforeWritingFiles() {
        when(mongoTemplate.findById("missing", ImageMetadata.class)).thenReturn(null);
        assertEquals(400, service.addImage("missing", file).getCode()); verify(gridFsTemplate, never()).store(any(), anyString(), anyString());
        ImageMetadata full = new ImageMetadata(); for (int i = 0; i < 50; i++) full.getImages().add(new com.example.springbootcoludecode.entity.ImageItem());
        when(mongoTemplate.findById("full", ImageMetadata.class)).thenReturn(full);
        assertEquals(400, service.addImage("full", file).getCode()); verify(gridFsTemplate, never()).store(any(), anyString(), anyString());
    }

    @Test
    void compensatesBothFilesWhenAtomicAppendLosesRace() {
        ImageMetadata existing = new ImageMetadata(); existing.setId("record"); when(mongoTemplate.findById("record", ImageMetadata.class)).thenReturn(existing);
        when(thumbnailService.process(file)).thenReturn(new ThumbnailService.ProcessedImage(new byte[]{1}, new byte[]{2}, "image/png", 1, 1)); when(gridFsTemplate.store(any(), anyString(), anyString())).thenReturn(new ObjectId(), new ObjectId());
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(ImageMetadata.class))).thenReturn(null);
        when(mongoTemplate.exists(any(Query.class), eq(ImageMetadata.class))).thenReturn(true);
        assertEquals(400, service.addImage("record", file).getCode()); verify(gridFsTemplate, times(2)).delete(any(Query.class));
    }

    @Test
    void listsImagesInArrayOrderAndReturnsFriendlyEmptyState() {
        ImageMetadata metadata = new ImageMetadata(); metadata.setId("record");
        com.example.springbootcoludecode.entity.ImageItem first = new com.example.springbootcoludecode.entity.ImageItem(); first.setImageId("first");
        com.example.springbootcoludecode.entity.ImageItem second = new com.example.springbootcoludecode.entity.ImageItem(); second.setImageId("second");
        metadata.getImages().add(first); metadata.getImages().add(second); when(mongoTemplate.findById("record", ImageMetadata.class)).thenReturn(metadata);
        ImagePageRequest page = new ImagePageRequest(); page.setInformationId("record"); page.setPage(1); page.setSize(1);
        @SuppressWarnings("unchecked") java.util.Map<String, Object> data = (java.util.Map<String, Object>) service.listImages(page).getData();
        assertEquals(2, data.get("total")); assertEquals("first", ((com.example.springbootcoludecode.dto.PublicImageResponse) ((java.util.List<?>) data.get("records")).get(0)).getImageId());
        ImageMetadata empty = new ImageMetadata(); empty.setId("empty"); when(mongoTemplate.findById("empty", ImageMetadata.class)).thenReturn(empty); page.setInformationId("empty"); assertEquals("该足迹没有图片", service.listImages(page).getMessage());
    }

    @Test
    void deletesDistinctMatchingImagesAndReportsIgnoredIds() {
        ImageMetadata before = new ImageMetadata(); before.setId("record");
        com.example.springbootcoludecode.entity.ImageItem first = new com.example.springbootcoludecode.entity.ImageItem(); first.setImageId("first"); first.setOriginalGridFsFileId(new ObjectId().toString()); first.setThumbnailGridFsFileId(new ObjectId().toString());
        before.getImages().add(first);
        ImageMetadata after = new ImageMetadata(); after.setId("record"); when(mongoTemplate.findById("record", ImageMetadata.class)).thenReturn(before, after);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(ImageMetadata.class))).thenReturn(after);
        ImageBatchDeleteRequest request = new ImageBatchDeleteRequest(); request.setInformationId("record"); request.setImageIds(java.util.Arrays.asList("first", "first", "foreign"));
        @SuppressWarnings("unchecked") java.util.Map<String, Object> data = (java.util.Map<String, Object>) service.deleteImages(request).getData();
        assertEquals(2, data.get("requestedCount")); assertEquals(1, data.get("deletedCount")); assertEquals(java.util.Collections.singletonList("foreign"), data.get("ignoredImageIds")); assertEquals(0, data.get("remainingCount"));
        verify(gridFsTemplate, times(2)).delete(any(Query.class));
    }

    @Test
    void readsOnlyTheImageOwnedByItsFootprintAndRejectsBlankResourceIdentifiers() throws Exception {
        ImageMetadata metadata = new ImageMetadata(); metadata.setId("record");
        com.example.springbootcoludecode.entity.ImageItem image = new com.example.springbootcoludecode.entity.ImageItem();
        image.setImageId("image-1"); image.setOriginalGridFsFileId(new ObjectId().toString()); image.setThumbnailGridFsFileId(new ObjectId().toString()); image.setContentType("image/png"); metadata.getImages().add(image);
        com.mongodb.client.gridfs.model.GridFSFile gridFsFile = new com.mongodb.client.gridfs.model.GridFSFile(
                new org.bson.BsonObjectId(new ObjectId()), "image.png", 2L, 261120, new java.util.Date(), new org.bson.Document());
        GridFsResource gridFsResource = mock(GridFsResource.class);
        when(mongoTemplate.findById("record", ImageMetadata.class)).thenReturn(metadata);
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(gridFsFile);
        when(gridFsTemplate.getResource(gridFsFile)).thenReturn(gridFsResource);
        when(gridFsResource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[]{3, 4}));

        assertArrayEquals(new byte[]{3, 4}, service.readOriginal("record", "image-1").getBytes());
        assertNull(service.readOriginal("other-record", "image-1"));
        assertNull(service.readOriginal("record", "other-image"));
        assertThrows(IllegalArgumentException.class, () -> service.readOriginal(" ", "image-1"));
    }
}
